use serde::Serialize;
use std::{
    path::Path,
    process::{Command, Output},
};

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

fn git(path: &Path, args: &[&str]) -> Result<Output, String> {
    let output = Command::new("git")
        .arg("-C")
        .arg(path)
        .args(args)
        .output()
        .map_err(|error| format!("Could not start Git: {error}"))?;

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
    let output = Command::new("git")
        .arg("-C")
        .arg(path)
        .args(["symbolic-ref", "--quiet", "--short", "HEAD"])
        .output()
        .map_err(|error| format!("Could not start Git: {error}"))?;

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
            &["log", "-n", "20", "--format=%h%x00%s%x00%an%x00%aI%x00"],
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
    use super::{current_branch, parse_commits};

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
