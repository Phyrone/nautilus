#!/bin/bash
set -euo pipefail

# Only needed in Claude Code on the web.
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

cd "${CLAUDE_PROJECT_DIR:-$(pwd)}"

# Rust (Cargo workspace): fetch crates and pre-build so tests/clippy start fast.
rustup component add rustfmt clippy
cargo fetch --locked
cargo build --workspace --locked

# pnpm workspace (docs site, future web UI).
pnpm install

# Gradle (JVM parts): download the wrapper and dependencies. Maven Central rate-limits the
# shared proxy (429), so use one worker and retry. Needs libraries.minecraft.net and
# repo.papermc.io in the environment's network allowlist; failure here is non-fatal.
for attempt in 1 2 3; do
  if ./gradlew --console=plain --max-workers=1 assemble -x test; then
    break
  fi
  echo "warning: gradle assemble failed (attempt $attempt)" >&2
  if [ "$attempt" -lt 3 ]; then
    sleep $((attempt * 20))
  fi
done || true
