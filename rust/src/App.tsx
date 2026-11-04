import "./App.css";
import { open } from "@tauri-apps/plugin-dialog";
import { invoke } from "@tauri-apps/api/core";
import { useRef, useState } from "react";

type RepositorySummary = {
  path: string;
  name: string;
  branch: string;
  commits: { id: string; subject: string; author: string; date: string }[];
};

function App() {
  const [repository, setRepository] = useState<RepositorySummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [opening, setOpening] = useState(false);
  const openingRef = useRef(false);

  async function openRepository() {
    if (openingRef.current) return;
    openingRef.current = true;
    setOpening(true);
    setError(null);
    try {
      const path = await open({ directory: true, multiple: false });
      if (!path || Array.isArray(path)) return;
      setRepository(await invoke<RepositorySummary>("open_repository", { path }));
    } catch (error) {
      setError(String(error));
    } finally {
      openingRef.current = false;
      setOpening(false);
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
              <h1>{repository.name}</h1>
              <p className="repository-path">{repository.path}</p>
            </div>
            <button className="button button-secondary" onClick={openRepository} disabled={opening}>
              {opening ? "Opening…" : "Open another"}
            </button>
          </div>
          <div className="branch-label">{repository.branch}</div>
          <h2>Latest 20 commits</h2>
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
        </section>
      ) : (
        <section className="welcome-panel">
          <h1>Open a repository</h1>
          <p>Choose a local Git repository to view its branch and recent commits.</p>
          <button className="button button-primary" onClick={openRepository} disabled={opening}>
            {opening ? "Opening…" : "Open repository"}
          </button>
          {error && <p className="error-message" role="alert">{error}</p>}
        </section>
      )}
    </main>
  );
}

export default App;
