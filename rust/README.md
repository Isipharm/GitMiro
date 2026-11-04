# GitMiro Rust port

Desktop shell uses Tauri v2, React/TypeScript, and Rust. Existing Kotlin application remains the behavior and visual reference during the migration.

## Development

```powershell
npm install
npm run tauri dev
```

The current slice opens an existing repository, then shows its current branch and the latest 20 commits. It does not persist recent repositories yet.

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
