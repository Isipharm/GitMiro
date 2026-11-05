use serde::Serialize;
use std::{
    io::{self, Read},
    path::Path,
    process::{Child, Command, Output, Stdio},
    thread,
};

const MAX_GIT_STDOUT_BYTES: usize = 512 * 1024;
const MAX_GIT_STDERR_BYTES: usize = 16 * 1024;
const COMMIT_PAGE_SIZE: usize = 500;

#[derive(Serialize)]
struct Commit {
    id: String,
    subject: String,
    author: String,
    date: String,
}

#[derive(Serialize)]
struct RepositorySummary {
    path: String,
    name: String,
    branch: String,
    history_head: Option<String>,
    commits: Vec<Commit>,
    has_more_commits: bool,
}

#[derive(Serialize)]
struct CommitPage {
    commits: Vec<Commit>,
    has_more_commits: bool,
}

fn read_capped(mut reader: impl Read, limit: usize) -> io::Result<(Vec<u8>, bool)> {
    let mut bytes = Vec::with_capacity(limit.min(8192));
    let mut truncated = false;
    let mut chunk = [0; 8192];
    loop {
        let read = reader.read(&mut chunk)?;
        if read == 0 {
            break;
        }
        let remaining = limit.saturating_sub(bytes.len());
        bytes.extend_from_slice(&chunk[..read.min(remaining)]);
        truncated |= read > remaining;
    }
    Ok((bytes, truncated))
}

fn collect_output(mut child: Child) -> Result<Output, String> {
    let stdout = child.stdout.take().expect("piped stdout");
    let stderr = child.stderr.take().expect("piped stderr");
    let stdout_reader = thread::spawn(move || read_capped(stdout, MAX_GIT_STDOUT_BYTES));
    let stderr_reader = thread::spawn(move || read_capped(stderr, MAX_GIT_STDERR_BYTES));
    let status = match child.wait() {
        Ok(status) => status,
        Err(error) => {
            let _ = child.kill();
            let _ = child.wait();
            let _ = stdout_reader.join();
            let _ = stderr_reader.join();
            return Err(format!("Could not wait for Git: {error}"));
        }
    };
    let (stdout, stdout_truncated) = stdout_reader
        .join()
        .map_err(|_| "Could not read Git output".to_owned())?
        .map_err(|error| format!("Could not read Git output: {error}"))?;
    let (mut stderr, stderr_truncated) = stderr_reader
        .join()
        .map_err(|_| "Could not read Git error output".to_owned())?
        .map_err(|error| format!("Could not read Git error output: {error}"))?;

    if stdout_truncated {
        return Err("Git output exceeded the 512 KiB limit".to_owned());
    }
    if stderr_truncated {
        stderr.extend_from_slice(b"\n[Git error output truncated]");
    }
    Ok(Output {
        status,
        stdout,
        stderr,
    })
}

fn git_output(path: &Path, args: &[&str], locale: Option<&str>) -> Result<Output, String> {
    let mut command = Command::new("git");
    command
        .arg("-C")
        .arg(path)
        .args(args)
        .stdout(Stdio::piped())
        .stderr(Stdio::piped());
    if let Some(locale) = locale {
        command.env("LC_ALL", locale);
    }
    let child = command
        .spawn()
        .map_err(|error| format!("Could not start Git: {error}"))?;
    collect_output(child)
}

fn git(path: &Path, args: &[&str]) -> Result<Output, String> {
    let output = git_output(path, args, None)?;

    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).trim().to_owned());
    }
    Ok(output)
}

fn git_text(path: &Path, args: &[&str]) -> Result<String, String> {
    git(path, args).map(|output| {
        String::from_utf8_lossy(&output.stdout)
            .trim_end_matches(['\r', '\n'])
            .to_owned()
    })
}

fn parse_commits(output: &str) -> Vec<Commit> {
    let fields: Vec<_> = output.split('\0').collect();
    fields
        .as_chunks::<4>()
        .0
        .iter()
        .filter(|fields| !fields[0].trim_matches(['\r', '\n']).is_empty())
        .map(|fields| Commit {
            id: fields[0].trim_matches(['\r', '\n']).to_owned(),
            subject: fields[1].trim_end().to_owned(),
            author: fields[2].trim_end().to_owned(),
            date: fields[3].trim_end_matches(['\r', '\n']).to_owned(),
        })
        .collect()
}

fn current_branch(path: &Path) -> Result<String, String> {
    let output = git_output(path, &["symbolic-ref", "--quiet", "--short", "HEAD"], None)?;

    if output.status.success() {
        return Ok(String::from_utf8_lossy(&output.stdout)
            .trim_end_matches(['\r', '\n'])
            .to_owned());
    }
    if output.stderr.is_empty() {
        return Ok("Detached HEAD".to_owned());
    }
    Err(String::from_utf8_lossy(&output.stderr).trim().to_owned())
}

fn repository_root(path: &Path) -> Result<String, String> {
    if !path.is_dir() {
        return Err("Selected path is not a directory".to_owned());
    }
    let output = git_output(path, &["rev-parse", "--show-toplevel"], Some("C"))?;
    if output.status.success() {
        return Ok(String::from_utf8_lossy(&output.stdout)
            .trim_end_matches(['\r', '\n'])
            .to_owned());
    }
    let error = String::from_utf8_lossy(&output.stderr);
    if error.contains("not a git repository") {
        return Err(
            "Failed to open repository, could not find a repository in the specified path"
                .to_owned(),
        );
    }
    Err(error.trim().to_owned())
}

fn history_head(path: &Path) -> Result<Option<String>, String> {
    let output = git_output(
        path,
        &["rev-parse", "--verify", "--quiet", "HEAD^{commit}"],
        None,
    )?;
    if output.status.success() {
        return Ok(Some(
            String::from_utf8_lossy(&output.stdout)
                .trim_end_matches(['\r', '\n'])
                .to_owned(),
        ));
    }
    if output.stderr.is_empty() {
        return Ok(None);
    }
    Err(String::from_utf8_lossy(&output.stderr).trim().to_owned())
}

fn is_commit_id(value: &str) -> bool {
    matches!(value.len(), 40 | 64) && value.bytes().all(|byte| byte.is_ascii_hexdigit())
}

fn commit_page(path: &Path, history_head: Option<&str>, skip: usize) -> Result<CommitPage, String> {
    let Some(history_head) = history_head else {
        return Ok(page_commits(Vec::new()));
    };
    if !is_commit_id(history_head) {
        return Err("Invalid history commit ID".to_owned());
    }

    let skip_arg = format!("--skip={skip}");
    let limit_arg = format!("--max-count={}", COMMIT_PAGE_SIZE + 1);
    let output = git_text(
        path,
        &[
            "log",
            &skip_arg,
            &limit_arg,
            history_head,
            "--format=%h%x00%<(256,trunc)%s%x00%<(256,trunc)%an%x00%aI%x00",
        ],
    )?;
    Ok(page_commits(parse_commits(&output)))
}

fn page_commits(mut commits: Vec<Commit>) -> CommitPage {
    let has_more_commits = commits.len() > COMMIT_PAGE_SIZE;
    commits.truncate(COMMIT_PAGE_SIZE);
    CommitPage {
        commits,
        has_more_commits,
    }
}

#[tauri::command]
async fn open_repository(path: String) -> Result<RepositorySummary, String> {
    tauri::async_runtime::spawn_blocking(move || {
        let selected = Path::new(&path);
        let root_path = repository_root(selected)?;
        let root = Path::new(&root_path);
        let branch = current_branch(root)?;
        let history_head = history_head(root)?;
        let page = commit_page(root, history_head.as_deref(), 0)?;

        Ok(RepositorySummary {
            path: root.display().to_string(),
            name: root
                .file_name()
                .and_then(|name| name.to_str())
                .unwrap_or("Repository")
                .to_owned(),
            branch,
            history_head,
            commits: page.commits,
            has_more_commits: page.has_more_commits,
        })
    })
    .await
    .map_err(|error| format!("Could not read repository: {error}"))?
}

#[tauri::command]
async fn load_more_commits(
    path: String,
    history_head: String,
    skip: usize,
) -> Result<CommitPage, String> {
    tauri::async_runtime::spawn_blocking(move || {
        let root_path = repository_root(Path::new(&path))?;
        commit_page(Path::new(&root_path), Some(&history_head), skip)
    })
    .await
    .map_err(|error| format!("Could not read repository: {error}"))?
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .invoke_handler(tauri::generate_handler![open_repository, load_more_commits])
        .run(tauri::generate_context!())
        .expect("failed to run GitMiro");
}

#[cfg(test)]
mod tests {
    use super::{
        COMMIT_PAGE_SIZE, Commit, MAX_GIT_STDERR_BYTES, collect_output, commit_page,
        current_branch, history_head, is_commit_id, page_commits, parse_commits, read_capped,
        repository_root,
    };

    #[test]
    fn caps_reader_output_and_reports_truncation() {
        let (bytes, truncated) = read_capped(&b"abcdef"[..], 4).unwrap();

        assert_eq!(bytes, b"abcd");
        assert!(truncated);
    }

    #[test]
    fn reports_a_clear_error_for_a_directory_without_a_git_repository() {
        use std::{
            fs,
            time::{SystemTime, UNIX_EPOCH},
        };

        let path = std::env::temp_dir().join(format!(
            "gitmiro-not-a-repository-{}-{}",
            std::process::id(),
            SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_nanos()
        ));
        fs::create_dir(&path).unwrap();

        let result = repository_root(&path);

        let _ = fs::remove_dir_all(&path);
        let error = result.unwrap_err();
        assert_eq!(
            error,
            "Failed to open repository, could not find a repository in the specified path"
        );
    }

    #[cfg(windows)]
    #[test]
    fn drains_both_child_pipes_before_waiting_for_exit() {
        use std::process::{Command, Stdio};

        let child = Command::new("powershell")
            .args([
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                "[Console]::Out.Write('o' * 131072); [Console]::Error.Write('e' * 65536)",
            ])
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .spawn()
            .unwrap();
        let output = collect_output(child).unwrap();

        assert!(output.status.success());
        assert_eq!(output.stdout.len(), 131072);
        assert_eq!(
            output.stderr.len(),
            MAX_GIT_STDERR_BYTES + b"\n[Git error output truncated]".len()
        );
    }

    #[test]
    fn parses_nul_delimited_git_log_records() {
        let output = format!(
            "abc123\0subject\u{1f}value\0Pierre\0{date}\0\ndef456\0Second\0Ada\0{date}\0\n",
            date = "2026-11-04T10:00:00+01:00"
        );
        let commits = parse_commits(&output);

        assert_eq!(commits.len(), 2);
        assert_eq!(commits[0].id, "abc123");
        assert_eq!(commits[0].subject, "subject\u{1f}value");
        assert_eq!(commits[0].author, "Pierre");
        assert_eq!(commits[0].date, "2026-11-04T10:00:00+01:00");
        assert_eq!(commits[1].id, "def456");
        assert_eq!(commits[1].author, "Ada");
    }

    #[test]
    fn paginates_commits_without_dropping_the_next_page_marker() {
        let commits = |count| {
            (0..count)
                .map(|id| Commit {
                    id: id.to_string(),
                    subject: String::new(),
                    author: String::new(),
                    date: String::new(),
                })
                .collect()
        };

        let page = page_commits(commits(COMMIT_PAGE_SIZE + 1));

        assert_eq!(page.commits.len(), COMMIT_PAGE_SIZE);
        assert!(page.has_more_commits);
        assert!(!page_commits(commits(COMMIT_PAGE_SIZE)).has_more_commits);
    }

    #[test]
    fn pagination_only_accepts_full_commit_ids() {
        assert!(is_commit_id(&"a".repeat(40)));
        assert!(is_commit_id(&"a".repeat(64)));
        assert!(!is_commit_id("HEAD~1"));
        assert!(!is_commit_id(&"g".repeat(40)));
    }

    #[test]
    fn pagination_stays_on_the_head_captured_when_repository_opens() {
        use std::{
            fs,
            process::Command,
            time::{SystemTime, UNIX_EPOCH},
        };

        let path = std::env::temp_dir().join(format!(
            "gitmiro-pinned-history-{}-{}",
            std::process::id(),
            SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_nanos()
        ));
        fs::create_dir(&path).unwrap();
        let run_git = |args: &[&str]| {
            let status = Command::new("git")
                .arg("-C")
                .arg(&path)
                .args(args)
                .status()
                .unwrap();
            assert!(status.success());
        };
        run_git(&["init", "--quiet"]);
        run_git(&["config", "user.name", "GitMiro Test"]);
        run_git(&["config", "user.email", "gitmiro-test@example.invalid"]);
        run_git(&["commit", "--allow-empty", "-m", "initial", "--quiet"]);
        let initial_head = history_head(&path).unwrap().unwrap();
        run_git(&["commit", "--allow-empty", "-m", "later", "--quiet"]);

        let page = commit_page(&path, Some(&initial_head), 0).unwrap();

        let _ = fs::remove_dir_all(&path);
        assert_eq!(page.commits.len(), 1);
        assert_eq!(page.commits[0].subject, "initial");
    }

    #[test]
    fn unborn_branch_is_not_reported_as_detached_head() {
        use std::{
            fs,
            process::Command,
            time::{SystemTime, UNIX_EPOCH},
        };

        let path = std::env::temp_dir().join(format!(
            "gitmiro-unborn-{}-{}",
            std::process::id(),
            SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_nanos()
        ));
        fs::create_dir(&path).unwrap();
        let init = Command::new("git")
            .args(["init", "--quiet"])
            .arg(&path)
            .status()
            .unwrap();
        assert!(init.success());

        let branch = current_branch(&path).unwrap();
        let _ = fs::remove_dir_all(&path);
        assert_ne!(branch, "Detached HEAD");
    }
}
