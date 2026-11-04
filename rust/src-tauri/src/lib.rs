use serde::Serialize;
use std::{
    io::{self, Read},
    path::Path,
    process::{Child, Command, Output, Stdio},
    thread,
};

const MAX_GIT_STDOUT_BYTES: usize = 256 * 1024;
const MAX_GIT_STDERR_BYTES: usize = 16 * 1024;

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
    commits: Vec<Commit>,
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
        return Err("Git output exceeded the 256 KiB limit".to_owned());
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

fn git_output(path: &Path, args: &[&str]) -> Result<Output, String> {
    let child = Command::new("git")
        .arg("-C")
        .arg(path)
        .args(args)
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .spawn()
        .map_err(|error| format!("Could not start Git: {error}"))?;
    collect_output(child)
}

fn git(path: &Path, args: &[&str]) -> Result<Output, String> {
    let output = git_output(path, args)?;

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
    output
        .split('\0')
        .collect::<Vec<_>>()
        .chunks_exact(4)
        .filter(|fields| !fields[0].trim_matches(['\r', '\n']).is_empty())
        .map(|fields| Commit {
            id: fields[0].trim_matches(['\r', '\n']).to_owned(),
            subject: fields[1].to_owned(),
            author: fields[2].to_owned(),
            date: fields[3].trim_end_matches(['\r', '\n']).to_owned(),
        })
        .collect()
}

fn current_branch(path: &Path) -> Result<String, String> {
    let output = git_output(path, &["symbolic-ref", "--quiet", "--short", "HEAD"])?;

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

#[tauri::command]
async fn open_repository(path: String) -> Result<RepositorySummary, String> {
    tauri::async_runtime::spawn_blocking(move || {
        let selected = Path::new(&path);
        if !selected.is_dir() {
            return Err("Selected path is not a directory".to_owned());
        }

        let root_path = git_text(selected, &["rev-parse", "--show-toplevel"])?;
        let root = Path::new(&root_path);
        let branch = current_branch(root)?;
        let log = git_text(
            root,
            &[
                "log",
                "-n",
                "20",
                "--format=%h%x00%<(256,trunc)%s%x00%<(256,trunc)%an%x00%aI%x00",
            ],
        )?;

        Ok(RepositorySummary {
            path: root.display().to_string(),
            name: root
                .file_name()
                .and_then(|name| name.to_str())
                .unwrap_or("Repository")
                .to_owned(),
            branch,
            commits: parse_commits(&log),
        })
    })
    .await
    .map_err(|error| format!("Could not read repository: {error}"))?
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .invoke_handler(tauri::generate_handler![open_repository])
        .run(tauri::generate_context!())
        .expect("failed to run GitMiro");
}

#[cfg(test)]
mod tests {
    use super::{MAX_GIT_STDERR_BYTES, collect_output, current_branch, parse_commits, read_capped};

    #[test]
    fn caps_reader_output_and_reports_truncation() {
        let (bytes, truncated) = read_capped(&b"abcdef"[..], 4).unwrap();

        assert_eq!(bytes, b"abcd");
        assert!(truncated);
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
