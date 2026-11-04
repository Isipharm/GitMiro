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
    git(path, args).map(|output| String::from_utf8_lossy(&output.stdout).trim().to_owned())
}

fn parse_commits(output: &str) -> Vec<Commit> {
    output
        .lines()
        .filter_map(|line| {
            let mut fields = line.splitn(4, '\x1f');
            Some(Commit {
                id: fields.next()?.to_owned(),
                subject: fields.next()?.to_owned(),
                author: fields.next()?.to_owned(),
                date: fields.next()?.to_owned(),
            })
        })
        .collect()
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
        let branch = git_text(root, &["rev-parse", "--abbrev-ref", "HEAD"])?;
        let log = git_text(
            root,
            &["log", "-n", "20", "--format=%h%x1f%s%x1f%an%x1f%aI"],
        )?;

        Ok(RepositorySummary {
            path: root.display().to_string(),
            name: root
                .file_name()
                .and_then(|name| name.to_str())
                .unwrap_or("Repository")
                .to_owned(),
            branch: if branch == "HEAD" {
                "Detached HEAD".to_owned()
            } else {
                branch
            },
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
    use super::parse_commits;

    #[test]
    fn parses_git_log_records_and_skips_malformed_lines() {
        let commits = parse_commits(
            "abc123\x1fInitial commit\x1fPierre\x1f2026-11-04T10:00:00+01:00\ninvalid",
        );

        assert_eq!(commits.len(), 1);
        assert_eq!(commits[0].id, "abc123");
        assert_eq!(commits[0].subject, "Initial commit");
        assert_eq!(commits[0].author, "Pierre");
        assert_eq!(commits[0].date, "2026-11-04T10:00:00+01:00");
    }
}
