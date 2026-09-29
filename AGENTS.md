# AGENTS.md

Guidance for coding agents working in this repository.

## Project

Nautilus is a Kubernetes-native Minecraft network platform (Velocity + Paper and its forks),
loosely inspired by CloudNet. It is **early and mostly unimplemented**.

**Read [ARCHITECTURE.md](ARCHITECTURE.md) before non-trivial work.** It describes the target
architecture, the resource model, the milestones, and how far the code is from them. Build
toward the target, not toward the legacy shape of the code. If a task conflicts with it, say so
instead of silently diverging; if a decision changes, update ARCHITECTURE.md in the same change.

## Repository layout

This is a polyglot monorepo: a Cargo workspace (control plane), a Gradle build (JVM parts)
and a pnpm workspace (web UI, docs site).

| Path                         | What                                                            | Build   |
|------------------------------|-----------------------------------------------------------------|---------|
| `lib/crds/src/main/rust`     | CRD types — **single source of truth** for the API              | Cargo   |
| `lib/crds/src/main/k8s`      | Generated CRD YAML — **do not edit by hand**                    | Cargo   |
| `lib/crds/build.gradle.kts`  | Generates Java types from the CRD YAML (for the builder)        | Gradle  |
| `services/operator`          | Kubernetes operator (kube-rs)                                   | Cargo   |
| `services/builder`           | Builds server images with Jib (stub)                            | Gradle  |
| `services/agent`             | Java agent (generic, attached to every server/proxy JVM) and the Paper/Velocity platform plugins; only empty shells so far | Gradle  |
| `services/provisioner`       | Init container preparing server pods (currently Kotlin/JGit; being rewritten in Rust, see ARCHITECTURE.md) | Gradle  |
| `proto/`                     | Protobuf/RPC contracts (agent ↔ gateway, agent API). Currently gRPC via `lib/grpc`; moving to ConnectRPC (buf for the JVM, `build.rs` for Rust) | Gradle  |
| `services/webui`             | Web UI (SvelteKit; accounts via better-auth → drizzle → postgres.js), not created yet | pnpm    |
| `examples/`                  | Example custom resources                                        | —       |
| `page/`                      | Docusaurus documentation site                                   | pnpm    |

Legacy modules scheduled for removal — do not build on them: `services/agent/bungee`,
`lib/k8s`, `lib/app-commons`, `lib/api-client`.

## Commands

Rust (stable toolchain, edition 2024):

```bash
cargo build --workspace --locked
cargo test --workspace --locked
cargo fmt --all
cargo clippy --workspace --locked
```

After changing anything in `lib/crds/src/main/rust`, regenerate the CRD YAML and commit it
(CI fails if it is outdated):

```bash
cargo run -p crds --bin generator -- export -o lib/crds/src/main/k8s
```

JVM (Gradle 9 wrapper, JDK 21 toolchain):

```bash
./gradlew build
./gradlew test
./gradlew :agent:shadowJar        # combined agent jar
```

Docs: `pnpm --dir page start`.

CI (`.github/workflows/build.yaml`) runs `cargo fmt --check`, build, test, clippy, the CRD
freshness check, and `./gradlew build test`. Run the relevant ones before calling work done.

## Running against a cluster

`cargo run -p operator` uses the current kubeconfig context and, by default, **applies the
CRDs cluster-wide on startup** (`--crds apply`). Never run it, the CRD generator's
`apply`/`delete`, or `kubectl` write commands without the user confirming the target context.
There is no local dev-cluster setup yet.

## Conventions

### General

- Batteries included, opt-out: the Helm chart installs everything by default, but the core
  (operator, builder, gateway, registry, NATS) must never depend on an optional part — the
  web UI, PostgreSQL/CloudNativePG, MariaDB, Valkey. When a resource needs a missing optional
  part, report it in a status condition.

### Rust

- Errors: `thiserror` for error types, `error-stack` `Report`s at application boundaries.
- Logging: `tracing`. No `println!` in services.
- Kubernetes writes use server-side apply with the shared field manager
  (`FIELD_MANAGER_NAME` in `services/operator/src/consts.rs`). Don't `replace` whole objects.
- Put label/annotation keys and default images in `consts.rs`, not inline strings.
- Reconcilers must be idempotent and must not assume they are the only writer.
- Workspace dependencies are declared once in the root `Cargo.toml`.
- CRD fields are `Option<_>` with documented defaults; doc comments become the CRD
  descriptions users see, so write them for users.

### Kotlin

- Code style: `kotlin.code.style=official` (ktlint runs but does not fail the build).
- Kotlin is only for the Java agent, the platform plugins and the builder. TypeScript only for
  the web UI. Everything else is Rust.
- The Java agent is generic: nothing Paper- or Velocity-specific goes into it. Platform
  specifics belong in the platform plugins, which only talk to the agent API.
- The Java agent sits on the system class path. Only its public API package may be visible
  there; relocate or isolate everything else (Kotlin stdlib, coroutines, protobuf, NATS, …) so it
  never clashes with libraries plugins bring.
- The Paper plugin must be Folia-safe: use region/entity schedulers, never assume a single
  main thread.
- The agent is a ConnectRPC server; the control plane calls it. Its only path to Kubernetes
  is the gateway, its only other outbound connection is NATS. No Kubernetes client in the JVM.
- The gateway is stateless (caches only) and must stay that way so it can scale.
- The builder's cache is shared by concurrent builds and builder replicas: write to a temp
  file, verify the hash, publish with an atomic rename, never modify a published entry.

### Commits

[Gitmoji](https://gitmoji.dev/) followed by an imperative summary, e.g.
`✨ Add MinecraftProxyFleet reconciler`, `🐛 Fix PVC claim of persistent servers`,
`⬆️ Update kube to v4.3`, `💥 Replace MinecraftServer replicas with fleets`.
Renovate handles dependency updates.

## License

EUPL-1.2. New files need no license header.
