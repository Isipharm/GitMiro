# ADR-001: Tauri v2 + React/TypeScript

- Status: Accepted for the Rust port direction; validate with the first vertical slice.
- Date: 2026-11-04
- Owner: GitMiro Rust Port

## Context

GitMiro needs a desktop UI that preserves the current Compose application's screens, interactions, themes, keyboard behavior, and Git workflows. The application engine and Git integration move to Rust and Git CLI. The user confirmed Tauri v2 with React/TypeScript as the selected UI stack.

## Decision

- Tauri v2 owns the desktop window, native integration, and a restricted IPC boundary.
- React/TypeScript renders the UI in the system WebView.
- Rust owns application behavior, Git CLI calls, persistence, and OS integration.
- Git remains the source of truth for repository state.

## Consequences

- Reuse CSS and HTML layout for close design-system fidelity; match existing Graphite colors, Geist typography, density, menus, and dialogs.
- Treat frontend input as untrusted at every IPC command; expose narrow commands and capabilities.
- Validate WebView2 installation/runtime, packaging, startup time, idle/active memory, and interaction latency on supported platforms.
- Keep Kotlin behavior and its UI as the parity reference until replacement is complete.
- Reopen framework selection only if a concrete compatibility, licensing, accessibility, or measured performance issue blocks this stack.

## Validation scope

No competing framework prototypes. Validate a small Tauri/React/Rust vertical slice against the current application, then measure representative flows and process-tree resource usage. Preserve every platform currently supported by the existing release configuration unless the owner approves a change.

## Windows baseline snapshot

- Run: 2026-11-05, Windows 11 build 26300, AMD Ryzen 7 7800X3D, 32 GiB RAM, Git 2.54.0.
- Corpus: temporary local repository with 1,000 commits, HEAD `9d8af30a5ee9376a7ab1921ac0f93bebe370a96c`; both apps opened the same repository. Tauri loaded both 500-commit pages.
- Builds: Tauri commit `f44fa385`; Kotlin source baseline `abde13a0` (GitMiro 2.0.1).
- Five process-tree samples, 3 s apart: Tauri median RSS 517.2 MiB / private bytes 300.4 MiB; Kotlin median RSS 391.4 MiB / private bytes 535.8 MiB. Tauri RSS is 32% higher; private bytes are 44% lower. Include descendant WebView2 processes in Tauri totals.
- One 20 s idle CPU window: Tauri consumed 0.24 CPU s; Kotlin consumed 0.00 CPU s at displayed precision. This single window is directional only.
- Visual comparison: Kotlin shows commit graph and status/workflow panels; Tauri currently shows a commit list. The React slice has no custom graph drawing, so graph draw cost cannot yet be measured. This confirms a major parity gap; it does not demonstrate an overall performance win.
- Follow-up after this baseline: Tauri now draws merge-aware lanes across local branches, remote branches, and tags, with lane state preserved across pages. Branch/tag labels, stashes, and status/workflow panels remain missing; the earlier performance snapshot does not measure graph drawing.
- Graph follow-up: on the same 1,000-commit fixture and machine at `5d03950e`, five idle samples 3 s apart measured the Tauri **debug** process tree (app + six WebView2 processes) at median RSS 506.3 MiB / private bytes 270.2 MiB. The Kotlin app JVM alone measured 280.2 MiB / 537.2 MiB; its Gradle launcher/daemon were excluded. CPU rose 0.22 s for Tauri and 0.03 s for Kotlin over the 12 s window. This is diagnostic only: debug build, one warm run, and RSS depends on resident pages; repeat with release builds and cold-start/interaction measurements before drawing a performance conclusion.
- Kotlin stores recent paths as JSON in Java Preferences (`gitmiroConfig` / `lastOpenedRepositoriesList`); this staged Tauri slice stores its own recent paths in WebView local storage. Import the existing Kotlin value during final settings migration before Kotlin is removed; do not mutate Kotlin settings during staging.
- CI run `41942059184` passed Rust format/lint/tests, Windows installers, macOS/Linux Tauri builds, and Gradle build/tests.
- Limits: one machine and one corpus; no cold-start, interaction-latency, accessibility/keyboard, active-load, or p50/p95 measurements. Do not use this snapshot to claim the port meets performance goals.
