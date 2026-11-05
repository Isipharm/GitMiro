import "./App.css";
import { open } from "@tauri-apps/plugin-dialog";
import { invoke } from "@tauri-apps/api/core";
import { useEffect, useRef, useState } from "react";

type RepositorySummary = {
  path: string;
  name: string;
  branch: string;
  history_head: string | null;
  commits: { id: string; subject: string; author: string; date: string }[];
  has_more_commits: boolean;
};

type CommitPage = Pick<RepositorySummary, "commits" | "has_more_commits">;

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
  const [opening, setOpening] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const openingRef = useRef(false);
  const loadingMoreRef = useRef(false);
  const repositoryHeadingRef = useRef<HTMLHeadingElement>(null);
  const recentRepositoryRefs = useRef<Array<HTMLButtonElement | null>>([]);

  useEffect(() => {
    if (repository) repositoryHeadingRef.current?.focus();
  }, [repository]);

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
        historyHead: repository.history_head,
        skip: repository.commits.length,
      });
      setRepository((current) => current?.path === repository.path && current.history_head === repository.history_head
        ? {
          ...current,
          commits: [...current.commits, ...page.commits],
          has_more_commits: page.has_more_commits,
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
          <h2>Commit history</h2>
          {error && <p className="error-message" role="alert">{error}</p>}
          {repository.commits.length ? (
            <ol className="commit-list">
              {repository.commits.map((commit) => (
                <li key={commit.id}>
                  <code>{commit.id}</code>
                  <span className="commit-subject">{commit.subject}</span>
                  <span className="commit-meta">{commit.author} · {new Date(commit.date).toLocaleString()}</span>
                </li>
              ))}
            </ol>
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
