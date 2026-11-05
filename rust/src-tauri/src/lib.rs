use serde::{Deserialize, Serialize};
use std::{
    collections::{BTreeMap, BTreeSet},
    io::{self, Read, Write},
    path::Path,
    process::{Child, Command, Output, Stdio},
    thread,
};

const MAX_GIT_STDOUT_BYTES: usize = 512 * 1024;
const MAX_GIT_STDERR_BYTES: usize = 16 * 1024;
const COMMIT_PAGE_SIZE: usize = 500;
const MAX_GRAPH_LANES: usize = 4096;

#[derive(Clone, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "snake_case")]
enum ReferenceKind {
    Local,
    Remote,
    Tag,
}

#[derive(Clone, Deserialize, Serialize)]
struct GitReference {
    id: String,
    name: String,
    kind: ReferenceKind,
}

#[derive(Clone, Deserialize, Serialize)]
struct HistoryRefs {
    tips: Vec<String>,
    references: Vec<GitReference>,
}

#[derive(Serialize)]
struct Commit {
    id: String,
    subject: String,
    author: String,
    date: String,
    parents: Vec<String>,
    lane: usize,
    passing_lanes: Vec<usize>,
    merging_lanes: Vec<usize>,
    forking_off_lanes: Vec<usize>,
    child_count: usize,
    references: Vec<GitReference>,
}

#[derive(Clone, Default, serde::Deserialize, Serialize)]
struct GraphState {
    reserved_lanes: BTreeMap<usize, String>,
}

#[derive(Serialize)]
struct RepositorySummary {
    path: String,
    name: String,
    branch: String,
    history_head: Option<String>,
    history_refs: HistoryRefs,
    status: RepositoryStatus,
    commits: Vec<Commit>,
    has_more_commits: bool,
    graph_state: GraphState,
}

#[derive(Default, Serialize)]
struct RepositoryStatus {
    staged: Vec<StatusEntry>,
    unstaged: Vec<StatusEntry>,
    truncated: bool,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
struct StatusEntry {
    path: String,
    path_bytes: Vec<u8>,
    kind: StatusKind,
}

#[derive(Clone, Copy, Debug, PartialEq, Serialize)]
#[serde(rename_all = "lowercase")]
enum StatusKind {
    Added,
    Modified,
    Removed,
    Conflicting,
}

#[derive(Serialize)]
struct CommitPage {
    commits: Vec<Commit>,
    has_more_commits: bool,
    graph_state: GraphState,
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
    collect_output_with_input(&mut child, None)
}

fn collect_output_with_input(child: &mut Child, input: Option<&[u8]>) -> Result<Output, String> {
    collect_output_inner(child, input, false).map(|(output, _)| output)
}

fn collect_output_with_truncated_stdout(child: Child) -> Result<(Output, bool), String> {
    let mut child = child;
    collect_output_inner(&mut child, None, true)
}

fn collect_output_inner(
    child: &mut Child,
    input: Option<&[u8]>,
    allow_truncated_stdout: bool,
) -> Result<(Output, bool), String> {
    let stdout = child.stdout.take().expect("piped stdout");
    let stderr = child.stderr.take().expect("piped stderr");
    let stdout_reader = thread::spawn(move || read_capped(stdout, MAX_GIT_STDOUT_BYTES));
    let stderr_reader = thread::spawn(move || read_capped(stderr, MAX_GIT_STDERR_BYTES));
    if let Some(input) = input {
        let result = child.stdin.take().expect("piped stdin").write_all(input);
        if let Err(error) = result {
            let _ = child.kill();
            let _ = child.wait();
            let _ = stdout_reader.join();
            let _ = stderr_reader.join();
            return Err(format!("Could not write Git input: {error}"));
        }
    }
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

    if stdout_truncated && !allow_truncated_stdout {
        return Err("Git output exceeded the 512 KiB limit".to_owned());
    }
    if stderr_truncated {
        stderr.extend_from_slice(b"\n[Git error output truncated]");
    }
    Ok((
        Output {
            status,
            stdout,
            stderr,
        },
        stdout_truncated,
    ))
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

fn git_output_with_input(path: &Path, args: &[&str], input: &[u8]) -> Result<Output, String> {
    let mut command = Command::new("git");
    command
        .arg("-C")
        .arg(path)
        .args(args)
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped());
    let mut child = command
        .spawn()
        .map_err(|error| format!("Could not start Git: {error}"))?;
    collect_output_with_input(&mut child, Some(input))
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

fn git_text_with_input(path: &Path, args: &[&str], input: &[u8]) -> Result<String, String> {
    let output = git_output_with_input(path, args, input)?;
    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).trim().to_owned());
    }
    Ok(String::from_utf8_lossy(&output.stdout)
        .trim_end_matches(['\r', '\n'])
        .to_owned())
}

fn parse_commits(output: &str) -> Vec<Commit> {
    let fields: Vec<_> = output.split('\0').collect();
    fields
        .as_chunks::<5>()
        .0
        .iter()
        .filter(|fields| !fields[0].trim_matches(['\r', '\n']).is_empty())
        .map(|fields| Commit {
            id: fields[0].trim_matches(['\r', '\n']).to_owned(),
            parents: fields[1].split_whitespace().map(str::to_owned).collect(),
            subject: fields[2].trim_end().to_owned(),
            author: fields[3].trim_end().to_owned(),
            date: fields[4].trim_end_matches(['\r', '\n']).to_owned(),
            lane: 0,
            passing_lanes: Vec::new(),
            merging_lanes: Vec::new(),
            forking_off_lanes: Vec::new(),
            child_count: 0,
            references: Vec::new(),
        })
        .collect()
}

fn parse_status(output: &[u8], truncated: bool) -> Result<RepositoryStatus, String> {
    let output = if truncated {
        output
            .iter()
            .rposition(|byte| *byte == 0)
            .map_or(&[][..], |end| &output[..=end])
    } else {
        output
    };
    let mut status = RepositoryStatus {
        truncated,
        ..RepositoryStatus::default()
    };
    let mut records = output.split(|byte| *byte == 0);
    while let Some(record) = records.next().filter(|record| !record.is_empty()) {
        let record_type = record[0];
        if record_type == b'#' || record_type == b'!' {
            continue;
        }
        if record_type == b'?' {
            let path = record
                .strip_prefix(b"? ")
                .ok_or_else(|| "Malformed Git status record".to_owned())?;
            status.unstaged.push(status_entry(path, StatusKind::Added));
            continue;
        }

        let field_count = match record_type {
            b'1' => 9,
            b'2' => 10,
            b'u' => 11,
            _ => return Err("Unsupported Git status record".to_owned()),
        };
        let fields = record
            .splitn(field_count, |byte| *byte == b' ')
            .collect::<Vec<_>>();
        if fields.len() != field_count
            || fields[1].len() != 2
            || fields.last().is_none_or(|p| p.is_empty())
        {
            return Err("Malformed Git status record".to_owned());
        }
        let original_path = if record_type == b'2' {
            match records.next().filter(|path| !path.is_empty()) {
                Some(path) => Some(path),
                None if truncated => break,
                None => return Err("Malformed Git rename record".to_owned()),
            }
        } else {
            None
        };

        let pair = fields[1];
        if record_type == b'u' || pair.contains(&b'U') {
            status.unstaged.push(status_entry(
                fields[field_count - 1],
                StatusKind::Conflicting,
            ));
            continue;
        }
        add_status_entry(
            &mut status.staged,
            pair[0],
            fields[field_count - 1],
            original_path,
        )?;
        add_status_entry(
            &mut status.unstaged,
            pair[1],
            fields[field_count - 1],
            original_path,
        )?;
    }
    status
        .staged
        .sort_by(|left, right| left.path_bytes.cmp(&right.path_bytes));
    status
        .unstaged
        .sort_by(|left, right| left.path_bytes.cmp(&right.path_bytes));
    Ok(status)
}

fn add_status_entry(
    entries: &mut Vec<StatusEntry>,
    code: u8,
    path: &[u8],
    original_path: Option<&[u8]>,
) -> Result<(), String> {
    if code == b'.' {
        return Ok(());
    }
    if code == b'R' {
        let original_path =
            original_path.ok_or_else(|| "Malformed Git rename record".to_owned())?;
        entries.push(status_entry(path, StatusKind::Added));
        entries.push(status_entry(original_path, StatusKind::Removed));
    } else {
        entries.push(status_entry(path, status_kind(code)));
    }
    Ok(())
}

fn status_entry(path: &[u8], kind: StatusKind) -> StatusEntry {
    StatusEntry {
        path: display_git_path(path),
        path_bytes: path.to_vec(),
        kind,
    }
}

fn display_git_path(path: &[u8]) -> String {
    use std::fmt::Write as _;

    let mut result = String::new();
    let mut remaining = path;
    while let Err(error) = std::str::from_utf8(remaining) {
        let valid = error.valid_up_to();
        append_display_text(
            &mut result,
            std::str::from_utf8(&remaining[..valid]).unwrap(),
        );
        let invalid = error
            .error_len()
            .unwrap_or_else(|| remaining.len().saturating_sub(valid));
        for byte in &remaining[valid..valid + invalid] {
            write!(result, "\\x{byte:02X}").unwrap();
        }
        remaining = &remaining[valid + invalid..];
    }
    append_display_text(&mut result, std::str::from_utf8(remaining).unwrap());
    result
}

fn append_display_text(output: &mut String, text: &str) {
    use std::fmt::Write as _;

    for character in text.chars() {
        match character {
            '\\' => output.push_str("\\\\"),
            '\n' => output.push_str("\\n"),
            '\r' => output.push_str("\\r"),
            '\t' => output.push_str("\\t"),
            character if character.is_control() => {
                write!(output, "\\u{{{:X}}}", character as u32).unwrap();
            }
            character => output.push(character),
        }
    }
}

fn status_kind(code: u8) -> StatusKind {
    match code {
        b'A' | b'C' | b'?' => StatusKind::Added,
        b'D' => StatusKind::Removed,
        _ => StatusKind::Modified,
    }
}

fn repository_status(path: &Path) -> Result<RepositoryStatus, String> {
    let mut command = Command::new("git");
    command
        .arg("-C")
        .arg(path)
        .args(["status", "--porcelain=v2", "-z", "--untracked-files=all"])
        .stdout(Stdio::piped())
        .stderr(Stdio::piped());
    let child = command
        .spawn()
        .map_err(|error| format!("Could not start Git: {error}"))?;
    let (output, truncated) = collect_output_with_truncated_stdout(child)?;
    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).trim().to_owned());
    }
    parse_status(&output.stdout, truncated)
}

fn graph_reservations_for(state: &mut GraphState, hash: &str) -> Vec<usize> {
    let lanes = state
        .reserved_lanes
        .iter()
        .filter_map(|(lane, reserved)| (reserved == hash).then_some(*lane))
        .collect::<Vec<_>>();
    for lane in &lanes {
        state.reserved_lanes.remove(lane);
    }
    lanes
}

fn next_available_lane(state: &GraphState, min: usize) -> usize {
    (min..)
        .find(|lane| !state.reserved_lanes.contains_key(lane))
        .unwrap()
}

fn validate_graph_state(state: &GraphState) -> Result<(), String> {
    if state.reserved_lanes.len() > MAX_GRAPH_LANES
        || state
            .reserved_lanes
            .iter()
            .any(|(lane, hash)| *lane >= MAX_GRAPH_LANES || !is_commit_id(hash))
    {
        return Err("Invalid commit graph cursor".to_owned());
    }
    Ok(())
}

fn layout_commits(commits: &mut [Commit], state: &mut GraphState) {
    for commit in commits {
        let reserved_lanes = graph_reservations_for(state, &commit.id);
        let child_count = reserved_lanes.len();
        let lane = reserved_lanes
            .first()
            .copied()
            .unwrap_or_else(|| next_available_lane(state, 0));
        let passing_lanes = state.reserved_lanes.keys().copied().collect::<Vec<_>>();
        let mut merging_lanes = Vec::new();

        if let Some(first_parent) = commit.parents.first() {
            state.reserved_lanes.insert(lane, first_parent.clone());
            for parent in commit.parents.iter().skip(1) {
                let existing = state
                    .reserved_lanes
                    .iter()
                    .find_map(|(lane, reserved)| (reserved == parent).then_some(*lane));
                let merge_lane = existing.unwrap_or_else(|| {
                    let merge_lane = next_available_lane(state, lane);
                    state.reserved_lanes.insert(merge_lane, parent.clone());
                    merge_lane
                });
                merging_lanes.push(merge_lane);
            }
        }

        commit.lane = lane;
        commit.passing_lanes = passing_lanes;
        commit.merging_lanes = merging_lanes;
        commit.forking_off_lanes = reserved_lanes.into_iter().skip(1).collect();
        commit.child_count = child_count;
    }
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

fn history_refs(path: &Path, head: Option<&str>) -> Result<HistoryRefs, String> {
    let refs = git_text(
        path,
        &[
            "for-each-ref",
            "--format=%(*objectname)%00%(*objecttype)%00%(objectname)%00%(objecttype)%00%(refname)",
            "refs/heads",
            "refs/remotes",
            "refs/tags",
        ],
    )?;
    let mut tips = BTreeSet::new();
    let mut references = Vec::new();
    if let Some(head) = head {
        tips.insert(head.to_owned());
    }
    for record in refs.lines() {
        let fields = record.split('\0').collect::<Vec<_>>();
        if fields.len() != 5 {
            continue;
        }
        let tip = if fields[1] == "commit" {
            fields[0]
        } else if fields[3] == "commit" {
            fields[2]
        } else {
            continue;
        };
        if is_commit_id(tip) {
            tips.insert(tip.to_owned());
            let refname = fields[4];
            let (kind, name) = if let Some(name) = refname.strip_prefix("refs/heads/") {
                (ReferenceKind::Local, name)
            } else if let Some(name) = refname.strip_prefix("refs/remotes/") {
                (ReferenceKind::Remote, name)
            } else if let Some(name) = refname.strip_prefix("refs/tags/") {
                (ReferenceKind::Tag, name)
            } else {
                continue;
            };
            references.push(GitReference {
                id: tip.to_owned(),
                name: name.to_owned(),
                kind,
            });
        }
    }
    references.sort_by(|left, right| left.name.cmp(&right.name));
    Ok(HistoryRefs {
        tips: tips.into_iter().collect(),
        references,
    })
}

fn is_commit_id(value: &str) -> bool {
    matches!(value.len(), 40 | 64) && value.bytes().all(|byte| byte.is_ascii_hexdigit())
}

fn commit_page(
    path: &Path,
    history_refs: &HistoryRefs,
    skip: usize,
    graph_state: GraphState,
) -> Result<CommitPage, String> {
    if history_refs.tips.is_empty() {
        return Ok(page_commits(Vec::new(), graph_state));
    }
    if history_refs.tips.iter().any(|tip| !is_commit_id(tip))
        || history_refs.references.iter().any(|reference| {
            !is_commit_id(&reference.id)
                || reference.name.is_empty()
                || reference.name.len() > 1024
                || reference.name.chars().any(char::is_control)
                || !history_refs.tips.contains(&reference.id)
        })
    {
        return Err("Invalid history commit ID".to_owned());
    }

    let skip_arg = format!("--skip={skip}");
    let limit_arg = format!("--max-count={}", COMMIT_PAGE_SIZE + 1);
    let revisions = history_refs.tips.join("\n");
    if revisions.len() > MAX_GIT_STDOUT_BYTES {
        return Err("Too many history references".to_owned());
    }
    let mut revisions = revisions.into_bytes();
    revisions.push(b'\n');
    let args = [
        "log",
        "--stdin",
        "--date-order",
        skip_arg.as_str(),
        limit_arg.as_str(),
        "--format=%H%x00%P%x00%<(256,trunc)%s%x00%<(256,trunc)%an%x00%aI%x00",
    ];
    let output = git_text_with_input(path, &args, &revisions)?;
    let mut commits = parse_commits(&output);
    for commit in &mut commits {
        commit.references = history_refs
            .references
            .iter()
            .filter(|reference| reference.id == commit.id)
            .cloned()
            .collect();
    }
    Ok(page_commits(commits, graph_state))
}

fn page_commits(mut commits: Vec<Commit>, mut graph_state: GraphState) -> CommitPage {
    let has_more_commits = commits.len() > COMMIT_PAGE_SIZE;
    commits.truncate(COMMIT_PAGE_SIZE);
    layout_commits(&mut commits, &mut graph_state);
    CommitPage {
        commits,
        has_more_commits,
        graph_state,
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
        let history_refs = history_refs(root, history_head.as_deref())?;
        let status = repository_status(root)?;
        let page = commit_page(root, &history_refs, 0, GraphState::default())?;

        Ok(RepositorySummary {
            path: root.display().to_string(),
            name: root
                .file_name()
                .and_then(|name| name.to_str())
                .unwrap_or("Repository")
                .to_owned(),
            branch,
            history_head,
            history_refs,
            status,
            commits: page.commits,
            has_more_commits: page.has_more_commits,
            graph_state: page.graph_state,
        })
    })
    .await
    .map_err(|error| format!("Could not read repository: {error}"))?
}

#[tauri::command]
async fn load_more_commits(
    path: String,
    history_refs: HistoryRefs,
    skip: usize,
    graph_state: GraphState,
) -> Result<CommitPage, String> {
    validate_graph_state(&graph_state)?;
    tauri::async_runtime::spawn_blocking(move || {
        let root_path = repository_root(Path::new(&path))?;
        commit_page(Path::new(&root_path), &history_refs, skip, graph_state)
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
        COMMIT_PAGE_SIZE, Commit, GraphState, MAX_GIT_STDERR_BYTES, StatusKind, collect_output,
        commit_page, current_branch, history_head, history_refs, is_commit_id, page_commits,
        parse_commits, parse_status, read_capped, repository_root, repository_status,
        validate_graph_state,
    };

    #[test]
    fn caps_reader_output_and_reports_truncation() {
        let (bytes, truncated) = read_capped(&b"abcdef"[..], 4).unwrap();

        assert_eq!(bytes, b"abcd");
        assert!(truncated);
    }

    #[test]
    fn parses_porcelain_v2_staged_unstaged_untracked_conflict_and_rename_paths() {
        let output = b"1 M. N... 100644 100644 100644 abc abc staged file.txt\0\
1 .M N... 100644 100644 100644 abc abc worktree.txt\0\
? new\nfile.txt\0\
u UU N... 100644 100644 100644 100644 abc abc abc conflict.txt\0\
2 R. N... 100644 100644 100644 abc abc R100 renamed file.txt\0old file.txt\0";

        let status = parse_status(output, false).unwrap();

        assert_eq!(status.staged.len(), 3);
        assert_eq!(status.staged[0].path, "old file.txt");
        assert!(matches!(status.staged[0].kind, StatusKind::Removed));
        assert_eq!(status.staged[1].path, "renamed file.txt");
        assert!(matches!(status.staged[1].kind, StatusKind::Added));
        assert_eq!(status.staged[2].path, "staged file.txt");
        assert!(matches!(status.staged[2].kind, StatusKind::Modified));
        assert_eq!(status.unstaged.len(), 3);
        assert_eq!(status.unstaged[0].path, "conflict.txt");
        assert!(matches!(status.unstaged[0].kind, StatusKind::Conflicting));
        assert_eq!(status.unstaged[1].path, "new\\nfile.txt");
        assert_eq!(status.unstaged[1].path_bytes, b"new\nfile.txt");
        assert!(matches!(status.unstaged[1].kind, StatusKind::Added));
        assert_eq!(status.unstaged[2].path, "worktree.txt");
        assert!(matches!(status.unstaged[2].kind, StatusKind::Modified));
    }

    #[test]
    fn rejects_malformed_porcelain_v2_records() {
        assert!(parse_status(b"1 M. missing fields\0", false).is_err());
        assert!(parse_status(b"2 R. missing original path\0", false).is_err());
    }

    #[test]
    fn preserves_non_utf8_paths_and_marks_capped_status_output() {
        let status = parse_status(b"? file-\xff.txt\0? file-\\xFF.txt\0? partial", true).unwrap();

        assert!(status.truncated);
        assert_eq!(status.unstaged.len(), 2);
        assert_eq!(status.unstaged[0].path, "file-\\\\xFF.txt");
        assert_eq!(status.unstaged[1].path, "file-\\xFF.txt");
        assert_eq!(status.unstaged[1].path_bytes, b"file-\xff.txt");
    }

    #[test]
    fn drops_incomplete_rename_from_capped_status_output() {
        let status = parse_status(
            b"? complete.txt\02 R. N... 100644 100644 100644 abc abc R100 target.txt\0",
            true,
        )
        .unwrap();

        assert_eq!(status.unstaged.len(), 1);
        assert_eq!(status.unstaged[0].path, "complete.txt");
    }

    #[test]
    fn reads_staged_unstaged_and_untracked_changes_from_a_temporary_repository() {
        use std::{
            fs,
            process::Command,
            time::{SystemTime, UNIX_EPOCH},
        };

        let path = std::env::temp_dir().join(format!(
            "gitmiro-status-{}-{}",
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
            assert!(status.success(), "git {args:?} failed");
        };
        run_git(&["init", "--quiet"]);
        fs::create_dir(path.join(".git/gitmiro-test-hooks")).unwrap();
        run_git(&["config", "core.hooksPath", ".git/gitmiro-test-hooks"]);
        run_git(&["config", "commit.gpgsign", "false"]);
        run_git(&["config", "user.name", "GitMiro Test"]);
        run_git(&["config", "user.email", "gitmiro-test@example.invalid"]);
        fs::write(path.join("tracked.txt"), "original\n").unwrap();
        run_git(&["add", "tracked.txt"]);
        run_git(&["commit", "--quiet", "-m", "initial"]);
        fs::write(path.join("tracked.txt"), "staged\n").unwrap();
        run_git(&["add", "tracked.txt"]);
        fs::write(path.join("tracked.txt"), "unstaged\n").unwrap();
        fs::write(path.join("new.txt"), "new\n").unwrap();

        let status = repository_status(&path).unwrap();
        let _ = fs::remove_dir_all(&path);

        assert_eq!(status.staged.len(), 1);
        assert_eq!(status.staged[0].path, "tracked.txt");
        assert!(matches!(status.staged[0].kind, StatusKind::Modified));
        assert_eq!(status.unstaged.len(), 2);
        assert_eq!(status.unstaged[0].path, "new.txt");
        assert!(matches!(status.unstaged[0].kind, StatusKind::Added));
        assert_eq!(status.unstaged[1].path, "tracked.txt");
        assert!(matches!(status.unstaged[1].kind, StatusKind::Modified));
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
            "abc123\0parent123 parent456\0subject\u{1f}value\0Pierre\0{date}\0\ndef456\0\0Second\0Ada\0{date}\0\n",
            date = "2026-11-04T10:00:00+01:00"
        );
        let commits = parse_commits(&output);

        assert_eq!(commits.len(), 2);
        assert_eq!(commits[0].id, "abc123");
        assert_eq!(commits[0].parents, ["parent123", "parent456"]);
        assert_eq!(commits[0].subject, "subject\u{1f}value");
        assert_eq!(commits[0].author, "Pierre");
        assert_eq!(commits[0].date, "2026-11-04T10:00:00+01:00");
        assert_eq!(commits[1].id, "def456");
        assert_eq!(commits[1].author, "Ada");
    }

    #[test]
    fn lays_out_merge_lanes_and_preserves_lane_state_across_pages() {
        let commit = |id: &str, parents: &[&str]| Commit {
            id: id.to_owned(),
            subject: String::new(),
            author: String::new(),
            date: String::new(),
            parents: parents.iter().map(|parent| (*parent).to_owned()).collect(),
            lane: 0,
            passing_lanes: Vec::new(),
            merging_lanes: Vec::new(),
            forking_off_lanes: Vec::new(),
            child_count: 0,
            references: Vec::new(),
        };

        let first_page = page_commits(
            vec![commit("merge", &["left", "right"])],
            GraphState::default(),
        );

        assert_eq!(first_page.commits[0].lane, 0);
        assert_eq!(first_page.commits[0].merging_lanes, [1]);
        assert_eq!(
            first_page.graph_state.reserved_lanes.get(&0).unwrap(),
            "left"
        );
        assert_eq!(
            first_page.graph_state.reserved_lanes.get(&1).unwrap(),
            "right"
        );

        let second_page = page_commits(
            vec![commit("left", &["right"]), commit("right", &[])],
            first_page.graph_state,
        );

        assert_eq!(second_page.commits[0].passing_lanes, [1]);
        assert_eq!(second_page.commits[1].child_count, 2);
        assert_eq!(second_page.commits[1].forking_off_lanes, [1]);
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
                    parents: Vec::new(),
                    lane: 0,
                    passing_lanes: Vec::new(),
                    merging_lanes: Vec::new(),
                    forking_off_lanes: Vec::new(),
                    child_count: 0,
                    references: Vec::new(),
                })
                .collect()
        };

        let page = page_commits(commits(COMMIT_PAGE_SIZE + 1), GraphState::default());

        assert_eq!(page.commits.len(), COMMIT_PAGE_SIZE);
        assert!(page.has_more_commits);
        assert!(!page_commits(commits(COMMIT_PAGE_SIZE), GraphState::default()).has_more_commits);
    }

    #[test]
    fn pagination_only_accepts_full_commit_ids() {
        assert!(is_commit_id(&"a".repeat(40)));
        assert!(is_commit_id(&"a".repeat(64)));
        assert!(!is_commit_id("HEAD~1"));
        assert!(!is_commit_id(&"g".repeat(40)));
    }

    #[test]
    fn rejects_invalid_graph_cursors_before_git_is_called() {
        let mut state = GraphState::default();
        state.reserved_lanes.insert(usize::MAX, "a".repeat(40));
        assert_eq!(
            validate_graph_state(&state).unwrap_err(),
            "Invalid commit graph cursor"
        );

        state.reserved_lanes.clear();
        state.reserved_lanes.insert(0, "invalid".to_owned());
        assert_eq!(
            validate_graph_state(&state).unwrap_err(),
            "Invalid commit graph cursor"
        );
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

        let refs = super::HistoryRefs {
            tips: vec![initial_head],
            references: Vec::new(),
        };
        let page = commit_page(&path, &refs, 0, GraphState::default()).unwrap();

        let _ = fs::remove_dir_all(&path);
        assert_eq!(page.commits.len(), 1);
        assert_eq!(page.commits[0].subject, "initial");
    }

    #[test]
    fn history_includes_unmerged_branches_and_peels_annotated_tags() {
        use std::{
            fs,
            process::Command,
            time::{SystemTime, UNIX_EPOCH},
        };

        let path = std::env::temp_dir().join(format!(
            "gitmiro-history-refs-{}-{}",
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
            assert!(status.success(), "git {args:?} failed");
        };
        run_git(&["init", "--quiet", "--initial-branch=main"]);
        run_git(&["config", "user.name", "GitMiro Test"]);
        run_git(&["config", "user.email", "gitmiro-test@example.invalid"]);
        run_git(&["commit", "--allow-empty", "-m", "base", "--quiet"]);
        run_git(&["checkout", "--quiet", "-b", "feature"]);
        run_git(&["commit", "--allow-empty", "-m", "feature", "--quiet"]);
        run_git(&["tag", "-a", "feature-release", "-m", "feature release"]);
        run_git(&["checkout", "--quiet", "main"]);
        run_git(&["commit", "--allow-empty", "-m", "main", "--quiet"]);

        let head = history_head(&path).unwrap().unwrap();
        let refs = history_refs(&path, Some(&head)).unwrap();
        let page = commit_page(&path, &refs, 0, GraphState::default()).unwrap();
        let subjects = page
            .commits
            .iter()
            .map(|commit| commit.subject.as_str())
            .collect::<Vec<_>>();

        let _ = fs::remove_dir_all(&path);
        assert!(subjects.contains(&"main"));
        assert!(subjects.contains(&"feature"));
        assert!(subjects.contains(&"base"));
        assert_eq!(refs.tips.len(), 2);
        assert!(
            refs.references
                .iter()
                .any(|reference| reference.name == "feature-release"
                    && reference.kind == super::ReferenceKind::Tag)
        );
        assert!(
            page.commits
                .iter()
                .find(|commit| commit.subject == "feature")
                .unwrap()
                .references
                .iter()
                .any(|reference| reference.name == "feature"
                    && reference.kind == super::ReferenceKind::Local)
        );
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
