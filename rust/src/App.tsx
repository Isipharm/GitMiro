import "./App.css";
import { open } from "@tauri-apps/plugin-dialog";
import { invoke } from "@tauri-apps/api/core";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { type CSSProperties, useEffect, useMemo, useRef, useState } from "react";
import { reachableCommitIds } from "./history-filter";

type GraphState = { reserved_lanes: Record<string, string> };

type RepositorySummary = {
  path: string;
  name: string;
  branch: string;
  history_head: string | null;
  history_refs: { tips: string[]; references: GitReference[] };
  commits: Commit[];
  has_more_commits: boolean;
  graph_state: GraphState;
  status: RepositoryStatus;
};

type RepositoryStatus = { staged: StatusEntry[]; unstaged: StatusEntry[]; truncated: boolean };
type StatusEntry = { path: string; path_bytes: number[]; kind: "added" | "modified" | "removed" | "conflicting" };
type StatusGroup = "staged" | "unstaged";
type FileDiff = { kind: "text" | "binary" | "submodule" | "conflict"; patch: string | null };

type Commit = {
  id: string;
  subject: string;
  author: string;
  date: string;
  parents: string[];
  lane: number;
  passing_lanes: number[];
  merging_lanes: number[];
  forking_off_lanes: number[];
  child_count: number;
  references: GitReference[];
};

type GitReference = { id: string; name: string; kind: "local" | "remote" | "tag" };

type CommitPage = Pick<RepositorySummary, "commits" | "has_more_commits" | "graph_state">;

const GRAPH_COLORS = ["#42a5f5", "#ef5350", "#78909c", "#ff7043", "#66bb6a", "#ec407a"];
const GRAPH_LANE_WIDTH = 18;
const COMMIT_ROW_HEIGHT = 50;

function samePathBytes(left: number[], right: number[]) {
  return left.length === right.length && left.every((byte, index) => byte === right[index]);
}

function CommitGraph({ commit }: { commit: Commit }) {
  const laneX = (lane: number) => GRAPH_LANE_WIDTH * (lane + 1);
  const centerY = COMMIT_ROW_HEIGHT / 2;
  const color = (lane: number) => GRAPH_COLORS[lane % GRAPH_COLORS.length];

  return (
    <svg className="commit-graph" aria-hidden="true">
      {commit.child_count > 0 && (
        <line x1={laneX(commit.lane)} y1="0" x2={laneX(commit.lane)} y2={centerY} stroke={color(commit.lane)} />
      )}
      {commit.passing_lanes.map((lane) => (
        <line key={`pass-${lane}`} x1={laneX(lane)} y1="0" x2={laneX(lane)} y2={COMMIT_ROW_HEIGHT} stroke={color(lane)} />
      ))}
      {commit.parents.length > 0 && (
        <line x1={laneX(commit.lane)} y1={centerY} x2={laneX(commit.lane)} y2={COMMIT_ROW_HEIGHT} stroke={color(commit.lane)} />
      )}
      {commit.forking_off_lanes.map((lane) => (
        <path
          key={`fork-${lane}`}
          d={`M ${laneX(commit.lane)} ${centerY} C ${laneX(commit.lane)} ${centerY - 14}, ${laneX(lane)} 14, ${laneX(lane)} 0`}
          stroke={color(lane)}
        />
      ))}
      {commit.merging_lanes.map((lane) => (
        <path
          key={`merge-${lane}`}
          d={`M ${laneX(commit.lane)} ${centerY} C ${laneX(commit.lane)} ${centerY + 14}, ${laneX(lane)} ${COMMIT_ROW_HEIGHT - 14}, ${laneX(lane)} ${COMMIT_ROW_HEIGHT}`}
          stroke={color(lane)}
        />
      ))}
      {commit.parents.length > 1 ? (
        <rect x={laneX(commit.lane) - 5} y={centerY - 5} width="10" height="10" rx="3" fill={color(commit.lane)} />
      ) : (
        <circle cx={laneX(commit.lane)} cy={centerY} r="5" fill={color(commit.lane)} />
      )}
    </svg>
  );
}

const RECENT_REPOSITORIES_KEY = "gitmiro.recentRepositories";

function readRecentRepositories(): string[] {
  try {
    const value: unknown = JSON.parse(localStorage.getItem(RECENT_REPOSITORIES_KEY) ?? "[]");
    return Array.isArray(value) && value.every((path) => typeof path === "string") ? value : [];
  } catch {
    return [];
  }
}

function App() {
  const [repository, setRepository] = useState<RepositorySummary | null>(null);
  const [recentRepositories, setRecentRepositories] = useState(readRecentRepositories);
  const [recentFilter, setRecentFilter] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [opening, setOpening] = useState(false);
  const [historyFilter, setHistoryFilter] = useState("");
  const [loadingMore, setLoadingMore] = useState(false);
  const [changingStatus, setChangingStatus] = useState(false);
  const [selectedDiff, setSelectedDiff] = useState<{ path: string; pathBytes: number[]; group: StatusGroup } | null>(null);
  const [fileDiff, setFileDiff] = useState<FileDiff | null>(null);
  const [diffLoading, setDiffLoading] = useState(false);
  const [diffError, setDiffError] = useState<string | null>(null);
  const selectedDiffRef = useRef<typeof selectedDiff>(null);
  const openingRef = useRef(false);
  const loadingMoreRef = useRef(false);
  const changingStatusRef = useRef(false);
  const diffRequestRef = useRef(0);
  const historyRequestRef = useRef(0);
  const statusRefreshRef = useRef(0);
  const repositoryHeadingRef = useRef<HTMLHeadingElement>(null);
  const recentRepositoryRefs = useRef<Array<HTMLButtonElement | null>>([]);

  useEffect(() => {
    if (repository) repositoryHeadingRef.current?.focus();
  }, [repository]);

  useEffect(() => {
    if (!repository) return;
    let active = true;
    let unlisten: (() => void) | undefined;
    void getCurrentWindow().onFocusChanged(({ payload: focused }) => {
      if (!focused || changingStatusRef.current) return;
      const refreshId = ++statusRefreshRef.current;
      void invoke<RepositoryStatus>("refresh_repository_status", { path: repository.path })
        .then((status) => {
          if (active && refreshId === statusRefreshRef.current) {
            setStatusError(null);
            setRepository((current) => current?.path === repository.path ? { ...current, status } : current);
          }
        })
        .catch((error) => {
          if (active && refreshId === statusRefreshRef.current) setStatusError(String(error));
        });
    }).then((stop) => {
      if (active) unlisten = stop;
      else stop();
    });
    return () => {
      active = false;
      unlisten?.();
    };
  }, [repository?.path]);

  useEffect(() => {
    if (!repository?.has_more_commits || loadingMore) return;
    const loadAtEnd = () => {
      if (document.documentElement.scrollHeight - window.innerHeight - window.scrollY < 200) {
        void loadMoreCommits();
      }
    };
    window.addEventListener("scroll", loadAtEnd);
    return () => window.removeEventListener("scroll", loadAtEnd);
  }, [repository?.path, repository?.commits.length, repository?.has_more_commits, loadingMore]);

  async function openRepository() {
    if (openingRef.current) return;
    openingRef.current = true;
    setOpening(true);
    setError(null);
    try {
      const path = await open({ directory: true, multiple: false });
      if (!path || Array.isArray(path)) return;
      await openRepositoryPath(path);
    } catch (error) {
      setError(String(error));
    } finally {
      openingRef.current = false;
      setOpening(false);
    }
  }

  async function openRepositoryPath(path: string) {
    const opened = await invoke<RepositorySummary>("open_repository", { path });
    historyRequestRef.current++;
    loadingMoreRef.current = false;
    setLoadingMore(false);
    setHistoryFilter("");
    statusRefreshRef.current++;
    setStatusError(null);
    diffRequestRef.current++;
    selectedDiffRef.current = null;
    setSelectedDiff(null);
    setFileDiff(null);
    setDiffError(null);
    setRepository(opened);
    setRecentRepositories((current) => [opened.path, ...current.filter((recent) => recent !== opened.path)]);
  }

  async function reopenRepository(path: string) {
    if (openingRef.current) return;
    openingRef.current = true;
    setOpening(true);
    setError(null);
    try {
      await openRepositoryPath(path);
    } catch (error) {
      setError(String(error));
    } finally {
      openingRef.current = false;
      setOpening(false);
    }
  }

  function removeRecentRepository(path: string) {
    setRecentRepositories((current) => current.filter((recent) => recent !== path));
  }

  useEffect(() => {
    try {
      localStorage.setItem(RECENT_REPOSITORIES_KEY, JSON.stringify(recentRepositories));
    } catch {
      // Recent paths are a convenience; unavailable storage must not block opening a repository.
    }
  }, [recentRepositories]);

  const filteredRecentRepositories = recentRepositories.filter((path) =>
    path.toLocaleLowerCase().includes(recentFilter.toLocaleLowerCase()),
  );

  const branchReference = repository?.history_refs.references.find(
    ({ kind, name }) => kind === "local" && name === historyFilter,
  );
  const reachableCommits = useMemo(() => {
    if (!repository || !historyFilter || !branchReference) return null;
    return reachableCommitIds(repository.commits, branchReference.id);
  }, [repository?.commits, branchReference, historyFilter]);

  async function loadMoreCommits() {
    if (!repository || !repository.has_more_commits || loadingMoreRef.current) return;
    const requestId = historyRequestRef.current;
    loadingMoreRef.current = true;
    setLoadingMore(true);
    setError(null);
    try {
      const page = await invoke<CommitPage>("load_more_commits", {
        path: repository.path,
        historyRefs: repository.history_refs,
        skip: repository.commits.length,
        graphState: repository.graph_state,
      });
      if (requestId === historyRequestRef.current) {
        setRepository((current) => current?.path === repository.path && current.history_head === repository.history_head
          ? {
            ...current,
            commits: [...current.commits, ...page.commits],
            has_more_commits: page.has_more_commits,
            graph_state: page.graph_state,
          }
          : current);
      }
    } catch (error) {
      if (requestId === historyRequestRef.current) setError(String(error));
    } finally {
      if (requestId === historyRequestRef.current) {
        loadingMoreRef.current = false;
        setLoadingMore(false);
      }
    }
  }

  async function viewFileDiff(entry: StatusEntry, group: StatusGroup) {
    if (!repository) return;
    const targetPath = repository.path;
    const requestId = ++diffRequestRef.current;
    const selection = { path: entry.path, pathBytes: entry.path_bytes, group };
    selectedDiffRef.current = selection;
    setSelectedDiff(selection);
    setFileDiff(null);
    setDiffError(null);
    setDiffLoading(true);
    try {
      const diff = await invoke<FileDiff>("get_file_diff", {
        path: targetPath,
        pathBytes: entry.path_bytes,
        group,
      });
      if (requestId === diffRequestRef.current) setFileDiff(diff);
    } catch (error) {
      if (requestId === diffRequestRef.current) setDiffError(String(error));
    } finally {
      if (requestId === diffRequestRef.current) setDiffLoading(false);
    }
  }

  function closeFileDiff() {
    diffRequestRef.current++;
    selectedDiffRef.current = null;
    setSelectedDiff(null);
    setFileDiff(null);
    setDiffError(null);
    setDiffLoading(false);
  }

  async function changeStatusEntry(entry: StatusEntry, action: "stage" | "unstage") {
    if (!repository || changingStatusRef.current) return;
    const targetPath = repository.path;
    changingStatusRef.current = true;
    setChangingStatus(true);
    const requestId = ++statusRefreshRef.current;
    setStatusError(null);
    try {
      const status = await invoke<RepositoryStatus>(`${action}_status_entry`, {
        path: targetPath,
        pathBytes: entry.path_bytes,
      });
      if (requestId === statusRefreshRef.current) {
        setRepository((current) => current?.path === targetPath ? { ...current, status } : current);
        const currentSelection = selectedDiffRef.current;
        if (currentSelection && samePathBytes(currentSelection.pathBytes, entry.path_bytes)) {
          const nextGroup = action === "stage" ? "staged" : "unstaged";
          const updatedEntry = status[nextGroup].find((candidate) => samePathBytes(candidate.path_bytes, entry.path_bytes));
          if (updatedEntry) void viewFileDiff(updatedEntry, nextGroup);
          else closeFileDiff();
        }
      }
    } catch (error) {
      if (requestId === statusRefreshRef.current) setStatusError(String(error));
    } finally {
      changingStatusRef.current = false;
      setChangingStatus(false);
    }
  }

  return (
    <main className="app-shell" aria-label="GitMiro">
      <header className="titlebar" data-tauri-drag-region>
        <span className="wordmark">GitMiro</span>
      </header>
      {repository ? (
        <section className="repository-view">
          <header className="repository-toolbar">
            <div className="repository-heading">
              <div>
                <h1 ref={repositoryHeadingRef} tabIndex={-1}>{repository.name}</h1>
                <p className="repository-path">{repository.path}</p>
              </div>
              <span className="branch-label">{repository.branch}</span>
            </div>
            <button className="button button-secondary" onClick={openRepository} disabled={opening || changingStatus}>
              {opening ? "Opening…" : "Open another"}
            </button>
          </header>
          <div className="repository-layout">
            <aside className="reference-sidebar" aria-label="Local branches">
              <h2>Local branches</h2>
              <ul>
                {repository.history_refs.references
                  .filter(({ kind }) => kind === "local")
                  .map(({ name }) => (
                    <li key={name}>
                      <button
                        className={name === historyFilter ? "reference-item reference-item-selected" : "reference-item"}
                        aria-pressed={name === historyFilter}
                        onClick={() => setHistoryFilter(historyFilter === name ? "" : name)}
                      >
                        <span className="reference-dot" aria-hidden="true" />
                        <span>{name}</span>
                      </button>
                    </li>
                  ))}
              </ul>
            </aside>
            <section className="history-panel" aria-label="Commit history">
              {selectedDiff ? (
                <div className="file-diff-view" aria-label="File diff">
                  <header className="file-diff-heading">
                    <button className="button button-secondary" onClick={closeFileDiff}>Back to history</button>
                    <div>
                      <h2 title={selectedDiff.path}>{selectedDiff.path}</h2>
                      <span>{selectedDiff.group === "staged" ? "Staged changes" : "Unstaged changes"}</span>
                    </div>
                  </header>
                  {diffLoading && <p className="muted" role="status">Loading diff…</p>}
                  {diffError && <p className="error-message" role="alert">{diffError}</p>}
                  {fileDiff?.kind === "binary" && <p className="muted" role="status">Binary file preview is not available.</p>}
                  {fileDiff?.kind === "conflict" && <p className="muted" role="status">Combined conflict preview. Side-by-side resolution is not available yet.</p>}
                  {fileDiff?.kind === "submodule" && <p className="muted" role="status">Submodule change</p>}
                  {fileDiff?.patch ? (
                    <pre className="file-diff-patch">{fileDiff.patch}</pre>
                  ) : fileDiff && fileDiff.kind !== "binary" && fileDiff.kind !== "conflict" ? (
                    <p className="muted">No diff available.</p>
                  ) : null}
                </div>
              ) : (
                <>
                  <div className="commit-history-heading">
                    <h2>Commit history</h2>
                    <label htmlFor="history-branch-filter">Branch</label>
                    <select
                      id="history-branch-filter"
                      value={historyFilter}
                      onChange={(event) => setHistoryFilter(event.target.value)}
                    >
                      <option value="">All branches</option>
                      {repository.history_refs.references
                        .filter(({ kind }) => kind === "local")
                        .map(({ name }) => <option value={name} key={name}>{name}</option>)}
                    </select>
                  </div>
                  {error && <p className="error-message" role="alert">{error}</p>}
                  {repository.commits.length ? (
                    <div className="commit-history" style={{ "--graph-width": `${Math.max(40, Math.max(...repository.commits.map(({ lane }) => lane)) * GRAPH_LANE_WIDTH + 36)}px` } as CSSProperties}>
                      <ol className="commit-list">
                        {repository.commits.map((commit) => (
                          <li className={reachableCommits && !reachableCommits.has(commit.id) ? "commit-dimmed" : undefined} key={commit.id}>
                            <CommitGraph commit={commit} />
                            <code>{commit.id.slice(0, 7)}</code>
                            <span className="commit-refs">
                              {commit.references.map((reference) => (
                                <span className={`ref-chip ref-${reference.kind}`} key={`${reference.kind}-${reference.name}`}>
                                  {reference.name}
                                </span>
                              ))}
                            </span>
                            <span className="commit-subject">{commit.subject}</span>
                            <span className="commit-meta">{commit.author} · {new Date(commit.date).toLocaleString()}</span>
                          </li>
                        ))}
                      </ol>
                    </div>
                  ) : <p className="muted">No commits yet.</p>}
                  {repository.has_more_commits && (
                    <button className="button button-secondary load-more" onClick={loadMoreCommits} disabled={loadingMore}>
                      {loadingMore ? "Loading…" : "Load older commits"}
                    </button>
                  )}
                </>
              )}
            </section>
            <aside className="status-column" aria-label="Repository status">
              {repository.status.truncated && (
                <p className="status-truncated" role="status">Change list is truncated at 512 KiB.</p>
              )}
              {statusError && <p className="error-message" role="alert">{statusError}</p>}
              <div className="status-panels">
                {(["staged", "unstaged"] as const).map((group) => {
                  const entries = repository.status[group];
                  const title = group === "staged" ? "Staged changes" : "Unstaged changes";
                  return (
                    <section className="status-pane" aria-label={title} key={group}>
                      <h2>{title} <span className="status-count">{entries.length}</span></h2>
                      {entries.length ? (
                        <ul>
                          {entries.map((entry) => (
                            <li key={`${entry.kind}-${entry.path_bytes.join(",")}`}>
                              <span className={`status-kind status-${entry.kind}`}>{entry.kind}</span>
                              <code title={entry.path}>{entry.path}</code>
                              <button
                                className="status-action"
                                type="button"
                                aria-label={`View diff for ${entry.path}`}
                                onClick={() => void viewFileDiff(entry, group)}
                              >
                                Diff
                              </button>
                              {group === "staged" ? (
                                <button
                                  className="status-action"
                                  type="button"
                                  aria-label={`Unstage ${entry.path}`}
                                  onClick={() => void changeStatusEntry(entry, "unstage")}
                                  disabled={changingStatus}
                                >
                                  Unstage
                                </button>
                              ) : (
                                <button
                                  className="status-action"
                                  type="button"
                                  aria-label={`${entry.kind === "conflicting" ? "Stage resolution for" : "Stage"} ${entry.path}`}
                                  onClick={() => void changeStatusEntry(entry, "stage")}
                                  disabled={changingStatus}
                                >
                                  {entry.kind === "conflicting" ? "Stage resolution" : "Stage"}
                                </button>
                              )}
                            </li>
                          ))}
                        </ul>
                      ) : <p className="muted">No changes.</p>}
                    </section>
                  );
                })}
              </div>
            </aside>
          </div>
        </section>
      ) : (
        <section className="welcome-panel">
          <h1>Open a repository</h1>
          <p>Choose a local Git repository to view its branch and recent commits.</p>
          <button className="button button-primary" onClick={openRepository} disabled={opening} autoFocus={recentRepositories.length === 0}>
            {opening ? "Opening…" : "Open repository"}
          </button>
          {error && <p className="error-message" role="alert">{error}</p>}
          <section className="recent-repositories" aria-label="Recent repositories">
            <label htmlFor="recent-filter">Recent</label>
            <input
              id="recent-filter"
              type="search"
              placeholder="Search recent repositories"
              value={recentFilter}
              onChange={(event) => setRecentFilter(event.target.value)}
              autoFocus={recentRepositories.length > 0}
              onKeyDown={(event) => {
                if (event.key === "ArrowDown" && filteredRecentRepositories.length > 0) {
                  event.preventDefault();
                  recentRepositoryRefs.current[0]?.focus();
                }
              }}
            />
            {/* shortcut: Keep all recent paths rendered; virtualize if measured list sizes slow the welcome screen. */}
            {recentRepositories.length === 0 ? (
              <p className="muted">No recent repositories.</p>
            ) : filteredRecentRepositories.length === 0 ? (
              <p className="muted">No matching repositories.</p>
            ) : (
              <ul>
                {filteredRecentRepositories.map((path, index) => (
                    <li key={path}>
                      <button
                        ref={(element) => { recentRepositoryRefs.current[index] = element; }}
                        className="recent-repository"
                        onClick={() => void reopenRepository(path)}
                        disabled={opening}
                        onKeyDown={(event) => {
                          if (event.key === "ArrowDown" && index < filteredRecentRepositories.length - 1) {
                            event.preventDefault();
                            recentRepositoryRefs.current[index + 1]?.focus();
                          } else if (event.key === "ArrowUp") {
                            event.preventDefault();
                            if (index === 0) document.getElementById("recent-filter")?.focus();
                            else recentRepositoryRefs.current[index - 1]?.focus();
                          }
                        }}
                      >
                        <strong>{path.split(/[\\/]/).filter(Boolean).slice(-1)[0] ?? path}</strong>
                        <span>{path}</span>
                      </button>
                      <button
                        className="recent-remove"
                        aria-label={`Remove ${path} from recent repositories`}
                        onClick={() => removeRecentRepository(path)}
                      >
                        ×
                      </button>
                    </li>
                ))}
              </ul>
            )}
          </section>
        </section>
      )}
    </main>
  );
}

export default App;
