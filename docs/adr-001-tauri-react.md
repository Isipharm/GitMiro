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
