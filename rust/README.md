# GitMiro Rust port

Desktop shell uses Tauri v2, React/TypeScript, and Rust. Existing Kotlin application remains the behavior and visual reference during the migration.

This is a staged replacement, not a second product: once the Rust port reaches parity and passes migration gates, Tauri becomes the main app and the Kotlin/Gradle application is removed.

## Development

```powershell
npm install
npm run tauri dev
```

The current slice opens an existing repository, then shows its current branch and commit history in batches of 500, loading older commits as the user scrolls. The welcome screen persists recent repository paths in the local WebView storage and supports search, reopen, and removal.

## Checks

```powershell
cargo fmt --check
cargo clippy -- -D warnings
cargo test
```

## Build

```powershell
npm run build
npm run tauri build
```
