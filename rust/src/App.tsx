import "./App.css";
import { open } from "@tauri-apps/plugin-dialog";
import { invoke } from "@tauri-apps/api/core";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { type CSSProperties, useEffect, useRef, useState } from "react";

type RepositorySummary = {
  path: string;
  name: string;
  branch: string;
  history_head: string | null;
  history_refs: { tips: string[]; references: GitReference[] };
  commits: Commit[];
  has_more_commits: boolean;
  graph_state: Record<string, string>;
  status: RepositoryStatus;
};

type RepositoryStatus = { staged: StatusEntry[]; unstaged: StatusEntry[]; truncated: boolean };
type StatusEntry = { path: string; path_bytes: number[]; kind: "added" | "modified" | "removed" | "conflicting" };

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
  const [loadingMore, setLoadingMore] = useState(false);
  const openingRef = useRef(false);
  const loadingMoreRef = useRef(false);
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
      if (!focused) return;
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
    setStatusError(null);
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

  async function loadMoreCommits() {
    if (!repository || !repository.has_more_commits || loadingMoreRef.current) return;
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
      setRepository((current) => current?.path === repository.path && current.history_head === repository.history_head
        ? {
          ...current,
          commits: [...current.commits, ...page.commits],
          has_more_commits: page.has_more_commits,
          graph_state: page.graph_state,
        }
        : current);
    } catch (error) {
      setError(String(error));
    } finally {
      loadingMoreRef.current = false;
      setLoadingMore(false);
    }
  }

  return (
    <main className="app-shell" aria-label="GitMiro">
      <header className="titlebar" data-tauri-drag-region>
        <span className="wordmark">GitMiro</span>
      </header>
      {repository ? (
        <section className="repository-view">
          <div className="repository-heading">
            <div>
              <div className="eyebrow">REPOSITORY</div>
              <h1 ref={repositoryHeadingRef} tabIndex={-1}>{repository.name}</h1>
              <p className="repository-path">{repository.path}</p>
            </div>
            <button className="button button-secondary" onClick={openRepository} disabled={opening}>
              {opening ? "Opening…" : "Open another"}
            </button>
          </div>
          <div className="branch-label">{repository.branch}</div>
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
                        </li>
                      ))}
                    </ul>
                  ) : <p className="muted">No changes.</p>}
                </section>
              );
            })}
          </div>
          <h2>Commit history</h2>
          {error && <p className="error-message" role="alert">{error}</p>}
          {repository.commits.length ? (
            <div className="commit-history" style={{ "--graph-width": `${Math.max(40, Math.max(...repository.commits.map(({ lane }) => lane)) * GRAPH_LANE_WIDTH + 36)}px` } as CSSProperties}>
              <ol className="commit-list">
                {repository.commits.map((commit) => (
                  <li key={commit.id}>
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
