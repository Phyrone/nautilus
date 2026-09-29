#!/bin/bash
set -euo pipefail

# Only needed in Claude Code on the web.
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

cd "${CLAUDE_PROJECT_DIR:-$(pwd)}"

# Rust (Cargo workspace): fetch crates and pre-build so tests/clippy start fast.
rustup component add rustfmt clippy >/dev/null 2>&1 || true
cargo fetch --locked
cargo build --workspace --locked

# pnpm workspace (docs site, future web UI).
pnpm install

# Gradle (JVM parts): download the wrapper distribution and dependencies.
./gradlew --console=plain --no-daemon assemble -x test || echo "warning: gradle assemble failed" >&2
