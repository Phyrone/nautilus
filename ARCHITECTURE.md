# Nautilus Architecture

> **Status:** target architecture. Most of it is not implemented yet — see
> [Current state](#current-state) for what exists today. When code and this document
> disagree, this document describes where we are going; update it when a decision changes.

## Vision

Nautilus is a Kubernetes-native Minecraft network platform, loosely inspired by CloudNet.

**Kubernetes is the cloud master.** A network — proxies, dynamic server fleets and
persistent servers — is declared as custom resources, and Nautilus runs it. Unlike a plain
Helm chart around a server image, Nautilus understands the *lifecycle* of a Minecraft server:
proxies discover servers automatically, scale-down never kills a server with players on it,
servers are drained before they stop, and plugins get a cloud API to find players and
servers across the network.

Both kinds of networks are first-class:

- **Static networks** — a few long-lived, persistent servers behind a proxy.
- **Dynamic networks** — lobbies and many short-lived minigame servers that scale with demand.

**Batteries included.** Kubernetes itself is modular; Nautilus is not meant to be assembled
by hand. One Helm chart installs a complete platform — registry, messaging, web UI, databases
for plugins — and every part beyond the core can be switched off. Nautilus is also meant to
be the base of a small ecosystem of plugins and tools built on its APIs.

## Principles

1. **Desired state lives in CRDs, and only there.** No external config database. Everything
   Nautilus creates can be rebuilt from the custom resources (GitOps-friendly).
2. **Live state never goes to etcd.** High-frequency data (players, counters) stays in the
   agents, which serve it on request, or in NATS. Only low-frequency lifecycle transitions
   are written to Kubernetes objects. See [Where data lives](#where-data-lives).
3. **The gateway is stateless.** It holds nothing but caches, so it scales horizontally and
   any replica can serve any request.
4. **The operator manages pods directly for fleets.** Deployments cannot know which pods are
   busy; lifecycle-aware scaling is the core of Nautilus.
5. **Agents never talk to Kubernetes directly.** Their only path to the Kubernetes API is the
   gateway; their only other outbound connection is NATS. The JVM contains no Kubernetes
   client and agent pods need no RBAC. Agents are themselves servers: the control plane
   calls them.
6. **Pods never download at startup.** Server software, plugins, the agent and templates are
   baked into an image by the builder. Pods only pull images; the kubelet caches them.
7. **Rust by default, Kotlin where the JVM ecosystem is needed** — the Java agent and
   platform plugins (they run inside the server) and the builder (Jib, JGit). The web UI is
   TypeScript (SvelteKit).
8. **Batteries included, opt-out.** Everything is installed by default; everything beyond
   the core can be disabled, or replaced by an existing installation (e.g. a CloudNativePG
   the cluster already runs). The core does not depend on the optional parts — it never
   assumes the web UI or a database exists.
9. **The core runs without a database.** Operator, builder and gateway keep no data outside
   CRDs, agents and NATS. PostgreSQL (CloudNativePG) is only used by optional parts: the web
   UI (accounts), PostgreSQL data services and features whose data is neither desired nor
   live state (history, audit, …).

## Scope

**v1 platforms:** Velocity (proxy), Paper and its forks — Paper, Purpur, Folia (server).
Plugins can be installed from Modrinth, Hangar and Spiget (Paper runs Spigot plugins).

**Out of scope for v1** (may come later): BungeeCord, Waterfall, Spigot/CraftBukkit as a
platform, Fabric/Forge/modded servers, Agones, world backups, multi-Kubernetes-cluster
networks.

## CloudNet → Nautilus

| CloudNet                        | Nautilus                                                        |
|---------------------------------|-----------------------------------------------------------------|
| Node / cluster of nodes         | Kubernetes + `operator`                                         |
| Task (dynamic services)         | `MinecraftServerFleet`, `MinecraftProxyFleet`                   |
| Static service                  | `MinecraftServer` (persistent)                                  |
| Template (local, S3, …)         | `MinecraftTemplate` (git, OCI, ConfigMap)                       |
| Group configuration             | Cluster defaults + ordered template layers                      |
| Service (`Lobby-1`)             | Pod (`lobby-1`)                                                 |
| Bridge module / Driver API      | Java agent API (player & service API) + NATS (channel messages) |
| REST module / web interface     | `webui` (SvelteKit)                                             |
| SyncProxy (MOTD, maintenance)   | Velocity plugin features (later)                                |

## Components

```
                  ┌──────────────────────── control plane ─────────────────────────┐
 kubectl / GitOps │  operator (Rust)         builder (Kotlin)      gateway (Rust)   │
 web UI ────────▶ │  • CRDs → Pods, Svcs,    • resolve versions    • stateless      │
                  │    PVCs, Secrets, NetPol • fetch + cache       • restricted,    │
                  │  • fleet scaling         • build images (Jib)    deduplicated   │
                  │  • creates Builds ─────▶ • push ──┐              k8s access     │
                  └───────┬───────────────────────────┼─────────────────▲──────────┘
                          │ manages pods,             ▼                 │ ConnectRPC
                          │ calls agents      internal registry         │ (agents → gateway)
                          │                          │ pull (kubelet)   │
               ┌──────────▼───────────┐       ┌──────▼──────────────────┴───────┐
  players ───▶ │ Proxy pod (Velocity) │ ────▶ │ Server pod (Paper)              │
               │  java agent          │ route │  init: provisioner (same image) │
               │  + Velocity plugin   │       │  main: java @argfile            │
               └──────────┬───────────┘       │        java agent + Paper plugin │
                          │                   └────────────────┬────────────────┘
                          └──── NATS / JetStream (messages) ───┘
```

| Component     | Language     | Location               | Responsibility                                                                                          |
|---------------|--------------|------------------------|---------------------------------------------------------------------------------------------------------|
| `crds`        | Rust         | `lib/crds`             | API types — single source of truth. Generates the CRD YAML, from which Kotlin types are generated for the builder. |
| `operator`    | Rust         | `services/operator`    | Reconciles CRDs, creates `MinecraftBuild`s, creates/deletes pods for fleets, scales them. Leader-elected. |
| `builder`     | Kotlin       | `services/builder`     | Watches `MinecraftBuild`s: resolves and fetches sources, builds images with Jib, pushes them, checks for updates. |
| `gateway`     | Rust         | `services/gateway`\*   | Stateless, horizontally scaled. The agents' restricted and deduplicated endpoint to Kubernetes: serves reads from shared watch caches, performs the few writes agents may do. See [Gateway](#gateway). |
| `provisioner` | Rust         | `services/provisioner` | Init container in every server/proxy pod: prepares the data volume and the launch arguments, then exits. |
| `agent`       | Kotlin       | `services/agent`       | Generic Java agent attached to every server/proxy JVM. Starts the server, owns the lifecycle, collects JVM metrics, serves the pod's endpoint (ConnectRPC server), reaches Kubernetes through the gateway and provides the Java/Kotlin API. See [Agent](#agent). |
| platform plugins | Kotlin    | `services/agent/{paper,velocity}` | Thin adapters: register the platform's features, services and metrics with the agent API and report readiness. The Paper plugin must be Folia-safe. |
| `webui`       | TypeScript (SvelteKit) | `services/webui`\* | Optional web interface for admins. Calls the Kubernetes API (custom resources) and the Nautilus APIs directly. See [Web UI](#web-ui). |
| protocol      | Protobuf, ConnectRPC | `proto/`       | All RPC contracts (agent server API, gateway API). ConnectRPC is gRPC-compatible. JVM code is generated with buf, Rust code in `build.rs`. |

\* Not created yet. The gateway can start as a subcommand of the operator binary (deployed
separately) and be split out later.

One installation serves every network in the Kubernetes cluster; state is partitioned by
`MinecraftCluster`.

### Installation

One Helm chart installs everything. The **core** is what a network needs to run; everything
else is installed by default and can be disabled (opt-out). Third-party parts can also be
replaced by an existing installation — two copies of the same operator in one cluster would
fight over the same CRDs.

| Part              | Role                                                         | Core | Without it                                 |
|-------------------|--------------------------------------------------------------|------|--------------------------------------------|
| operator, builder, gateway | Control plane                                       | yes  | —                                          |
| OCI registry      | Server images built by the builder                           | yes  | Admins bring their own registry            |
| NATS + JetStream  | Messaging between agents and the control plane               | yes  | Admins bring their own NATS                |
| `webui`           | Web interface                                                | no   | Manage with `kubectl` / GitOps             |
| CloudNativePG     | PostgreSQL: web UI accounts, PostgreSQL data services        | no   | Existing CloudNativePG or an external PostgreSQL for the web UI; no PostgreSQL data services |
| mariadb-operator  | MariaDB data services                                        | no   | Existing mariadb-operator, or no MariaDB data services |
| Valkey backend    | Valkey data services                                         | no   | No Valkey data services                    |

The operator detects which optional parts are present (e.g. by their CRDs) and reports a
clear status condition when a resource needs one that is missing, instead of failing
silently.

## Resource model

API group `nautilus.phyrone.de`, version `v1alpha1`. All resources are namespaced.

| Kind                   | Purpose                                                                                                        |
|------------------------|----------------------------------------------------------------------------------------------------------------|
| `MinecraftCluster`     | A network. Owns the Velocity forwarding secret, network-wide defaults (JVM flags, templates) and its NetworkPolicy. |
| `MinecraftProxyFleet`  | Velocity proxies of a network, exposed via a `LoadBalancer`/`NodePort` Service.                                |
| `MinecraftServerFleet` | Interchangeable, ephemeral servers (lobbies, minigames). Min/max replicas and a scaling policy.                |
| `MinecraftServer`      | A single named, persistent server (e.g. survival), backed by a Pod and a PVC.                                  |
| `MinecraftTemplate`    | A reusable layer of files: git or OCI (baked in at build time), or ConfigMap (applied at runtime).             |
| `MinecraftBuild`       | One image build: the inputs, the resolved lock and the resulting image digest. Created by the operator.        |
| `MinecraftDataService` | A database for plugins (PostgreSQL, MariaDB, Valkey), bound to networks, fleets or servers. See [Data services](#data-services). |

Every fleet and server references its network via `spec.cluster`. Every resource reports
`status.conditions` and `status.observedGeneration`, and emits Events.

### Scoping and sharing

A namespace may contain **many networks**. Some resources are shared by all networks in a
namespace, others belong to one network:

| Shared per namespace                                        | Per network (`MinecraftCluster`)                       |
|-------------------------------------------------------------|--------------------------------------------------------|
| `MinecraftTemplate`s (any network may use them)             | Velocity forwarding secret                             |
| `MinecraftBuild`s (identical inputs → one build, one image) | NetworkPolicy (only its proxies may reach its servers) |
| `MinecraftDataService`s (bound where needed)                | Gateway scope (an agent only sees and changes its own network) |

The builder's download and layer cache is installation-wide: it is content-addressed and
holds public artifacts, so sharing it across namespaces is safe. Private inputs (template
repositories, credentials) are only read from the build's own namespace.

Objects created per network are prefixed with the cluster name and carry the
`nautilus.phyrone.de/cluster` label.

### Owned objects

- **Fleets:** the operator creates Pods directly (owner = fleet). Pods get CloudNet-style
  ordinal names (`lobby-1`, `lobby-2`, …), reusing the lowest free number.
- **`MinecraftServer`:** one Pod plus one PVC. The PVC is *retained* when the server is
  deleted unless the spec asks otherwise — deleting a resource must never silently delete a
  world.
- **Proxy fleets:** Pods plus the public Service.
- **Builds:** the operator creates a `MinecraftBuild` per distinct set of inputs and
  references its image digest from the pods it creates.
- All writes use server-side apply with a single field manager.

## Server lifecycle

```
Pending → Starting → Ready ⇄ InUse → Draining → Stopped
```

| State      | Meaning                                                                  |
|------------|--------------------------------------------------------------------------|
| `Pending`  | Pod created, not scheduled, or the provisioner is running.               |
| `Starting` | Server process is booting.                                               |
| `Ready`    | Accepts players; registered on the proxies.                              |
| `InUse`    | Has players or is allocated (e.g. a running match).                      |
| `Draining` | No new players; existing ones are moved away or the round finishes.      |
| `Stopped`  | Process exited. Fleet pods are then deleted and replaced as needed.      |

The Java agent owns the state: it reports `Starting` as soon as the JVM runs, and `Ready`
once the platform plugin (and any plugin that registered a readiness check) reports ready.
It writes transitions through the gateway onto its own Pod (label
`nautilus.phyrone.de/state`), so `kubectl get pods` shows it and the operator can act on it.
Player counts stay in the agent, which serves them on request.

**Scaling rule:** scale-down only removes `Ready` pods without players. `InUse` pods are
drained first. A fleet's scaling policy can keep a buffer of empty `Ready` servers.

**Stopping:** the operator drains a pod by calling its agent *before* deleting it. After that,
Kubernetes sends SIGTERM and the server shuts down (and saves) through its own shutdown hook.
There is no supervisor process in the pod.

## Images, templates and provisioning

Nautilus replaces the itzg images with its own stack. Everything a server needs is an
ordered stack of layers; the builder bakes the stable part into an image, the provisioner
adds the per-pod part at startup.

### Templates

A `MinecraftTemplate` is a layer of files (configs, worlds, datapacks, …). Servers, fleets
and cluster defaults reference templates in order; later layers win.

| Source      | Applied        | Use                                                                   |
|-------------|----------------|-----------------------------------------------------------------------|
| `git`       | at build time  | Repo + ref + path. Version control, review, users' own workflows.    |
| `oci`       | at build time  | An image pushed by the user's own pipeline. Recommended for large content like worlds. |
| `configMap` | at runtime     | Small text configs. Changes apply without a rebuild.                  |

Pods never clone git or fetch templates themselves.

Server software, plugins, the Java agent and the platform plugins are **not** template
content. They are declared in
the spec (`install`) and resolved by the builder, so they can be pinned, deduplicated and
cached.

### Builder

For each `MinecraftBuild` the builder:

1. **Resolves** every input to an exact version: git ref → commit, OCI tag → digest,
   `latest` → a concrete Paper build and plugin versions. The result is the *lock*, stored in
   the build's status.
2. **Fetches** everything through its content-addressed cache, verifying hashes.
3. **Pre-patches paperclip** (`-Dpaperclip.patchonly=true`), so pods never download the
   Mojang server jar.
4. **Assembles the image with Jib**, least-changing layers first, so fleets share layers:
   JRE base + provisioner → server software → plugins → Java agent + platform plugin →
   templates (one layer each) → build-time config.
5. **Pushes** it to the internal registry and reports the digest in the build's status.

Builds are reproducible: identical locks produce identical digests, so a build whose lock
already exists is skipped. The builder periodically re-resolves inputs according to each
input's update policy (pinned, patch updates, latest); a changed lock produces a new image.
Images no longer referenced by any build are garbage-collected.

The base images (JRE per required Java version, plus the provisioner binary) are published
by the project.

### Rollouts

When a server's image or runtime ConfigMaps change (the operator puts their hash on the pod):

- **Fleets:** new pods use the new version; old pods keep running and drain naturally.
  No forced restarts.
- **Persistent servers:** the change applies on the next restart.

### Pod layout

```
init container  provisioner   image: <build>@<digest>   volume /data (emptyDir or PVC)
main container  java @argfile image: <build>@<digest>   volume /data
```

The **provisioner** runs from the same image as the server, so it can see the baked files.
It:

1. **Syncs** the baked server tree from the image into `/data`. For persistent servers it
   keeps a manifest of the files it manages: managed files (jars, template configs) are
   updated or removed, the world and other server-written data are never touched.
2. **Applies runtime layers** (ConfigMaps).
3. **Patches configs** declaratively: set keys in `.properties`/YAML/TOML, replace
   placeholders with values from Secrets and the downward API (server name, addresses,
   forwarding secret, gateway address).
4. **Runs scripts** from templates, as an escape hatch for everything else.
5. **Writes the JVM argfile**: memory from the pod's resources, flags, `-javaagent` for the
   Nautilus agent, and the server to start.
6. **Exits.** A failure fails the pod start visibly.

The **main container** runs `java @argfile` directly. The Java agent starts the server and
serves everything that needs the running server — health and readiness probes, metrics,
the API (see [Agent](#agent)).

### Builder cache and concurrency

The builder's download and layer cache is shared by every build of the installation, and
must stay correct with **several builds running at once, in one builder process or across
several builder replicas**:

- **Entries are immutable and content-addressed.** A download goes to a temporary file, is
  verified against its hash and published with an atomic rename. Concurrent downloads of
  the same artifact are harmless (same content, same name), and an entry is never modified
  after it is published.
- **In-flight downloads are deduplicated** where possible (one fetch per hash at a time), but
  correctness never depends on it.
- **Garbage collection must not delete entries in use** by a running build (mark/lease
  entries, or collect only entries unused for longer than any build can take).
- **Each `MinecraftBuild` is worked on by one process at a time**, claimed through its status
  with optimistic concurrency (resourceVersion) or a Lease.
- With more than one builder replica, the cache volume must be `ReadWriteMany`, or each
  replica keeps its own cache.

### Registry

Nautilus ships its own registry, deployed by the Helm chart by default. The builder pushes
to it and the kubelet on every node pulls from it. Admins can opt out and configure their
own registry instead (credentials for pushing, `imagePullSecrets` for pulling).

The kubelet pulls over the node's network, not the cluster's, so the bundled registry cannot
simply be addressed by its in-cluster Service name; the Helm chart must expose it to the
nodes (see [Open questions](#open-questions)).

## Communication

- **Protocol:** all RPC is ConnectRPC (gRPC-compatible), defined in `proto/`. Bidirectional
  streams need HTTP/2, which is available inside the cluster.
- **Agents are servers.** Each agent serves a ConnectRPC API in its pod. Whoever needs
  something from a running server calls it: the operator drains pods and reads player counts
  for scaling; other agents call it for cross-network features.
- **Agent → gateway:** agents call the gateway for everything that lives in Kubernetes —
  watching the network's servers, writing their own lifecycle state.
- **Authentication:** agents present their projected ServiceAccount token. The gateway checks
  it with the TokenReview API and maps the pod to its network. No shared secrets.
- **Proxy registration:** the Velocity plugin watches the network's servers through the
  gateway and registers and unregisters them dynamically. There is no static server list in
  `velocity.toml`.
- **Operator → agents:** drain, player counts. Lifecycle states the operator reads from the
  Pods.
- **Operator ↔ builder:** only through `MinecraftBuild` resources.
- **Messaging:** chosen per case.
  - *Direct call* (ConnectRPC) when the target is known and an answer is needed — e.g.
    "drain this pod", "send this player to server X".
  - *NATS* (core) for fan-out and fire-and-forget — channel messages between plugins,
    events.
  - *NATS JetStream* when messages or data must survive — durable event streams, work
    queues, and key/value buckets for shared live data such as player locations.

  Each network gets its own isolated NATS namespace (subject prefix or account), so a
  network can never read another network's messages.

## Where data lives

| Data                                                    | Stored in                                   |
|---------------------------------------------------------|---------------------------------------------|
| Desired state (networks, fleets, servers, templates)    | CRDs (etcd)                                 |
| Lifecycle state (low frequency)                         | Pod labels/conditions (etcd), via the gateway |
| Live data of one server (players, TPS, …)               | That server's agent, served on request      |
| Shared live data (e.g. player → server)                 | NATS JetStream key/value                    |
| Messages and events                                     | NATS / JetStream                            |
| Web UI accounts and sessions                            | PostgreSQL (CloudNativePG)                  |
| Durable data of optional features (history, audit, …)   | PostgreSQL (CloudNativePG)                  |
| Plugin data (permissions, economy, stats, …)            | Data services (PostgreSQL, MariaDB, Valkey) |

## Gateway

The gateway is the agents' only endpoint to Kubernetes. It is **stateless** — it holds only
caches, which any replica rebuilds on start — so it runs as a normal horizontally scaled
Deployment.

- **Restricted:** it exposes a small, purpose-built API instead of the Kubernetes API. An
  agent only sees its own network and may only change its own pod (lifecycle state). The
  broad RBAC stays with the gateway, never with agent pods.
- **Deduplicated:** reads are served from shared watch caches. Thousands of agents watching
  their network's servers cost one watch per gateway replica on the API server, not one per
  agent.

## Agent

### Java agent

Every server and proxy JVM runs the Nautilus **Java agent**, attached with `-javaagent` in
the argfile. It is generic — nothing in it is specific to Paper or Velocity. It:

- **starts the server** — it runs before the platform and then launches it, so it is up
  (and reporting `Starting`) before the platform has loaded;
- **owns the lifecycle state** and writes it through the gateway;
- **collects generic metrics** (memory, GC, threads, CPU);
- **has JVM instrumentation access** (`java.lang.instrument`) for hooks where no platform
  API exists;
- **serves the pod's endpoint** — the agent's ConnectRPC server (below);
- **provides the Java/Kotlin API** used by the platform plugins and third-party plugins.

**Class loading:** the agent jar is on the system class path, which every plugin class
loader can see. Only the public API package may be visible there. The implementation and
all its dependencies (Kotlin stdlib, coroutines, protobuf, ConnectRPC) are relocated or
loaded in an isolated class loader, so they never clash with the versions plugins bring.

### Platform plugins

The Paper and Velocity plugins are thin adapters. They use the agent API to register what
the platform provides — features, services, metrics (players, TPS/MSPT, …) — and report the
server ready. The Velocity plugin also registers the network's servers, which it watches through the
gateway.
The Paper plugin must be Folia-safe (region/entity schedulers, no single main thread).

Supporting a new platform means writing a new adapter plugin, not a new agent.

### Endpoint

One HTTP server per pod, served by the Java agent. It is the pod's only management
interface:

- **Health and readiness** — used by the Kubernetes probes. Readiness follows the lifecycle
  (`Ready`/`InUse` only).
- **Metrics** — Prometheus format: the agent's JVM metrics plus everything plugins registered.
- **API** — ConnectRPC, for interacting with the running server or proxy (e.g. players,
  commands, state).

### Developer API

Plugins get a Java/Kotlin API from the agent, roughly equivalent to CloudNet's Driver/Bridge
API:

- register features, services, metrics and readiness checks
- list servers and fleets of the network, with state and player counts
- look up a player (which proxy, which server)
- send a player to a server or to a fleet ("any free lobby")
- request a server from a fleet (allocation, e.g. for a match)
- publish/subscribe channel messages across the network
- lifecycle events (server started, player joined network, …)

## Web UI

A SvelteKit app, deployed in the cluster as its own service and exposed to admins (e.g.
via Ingress). Its server side talks directly to:

- the **Kubernetes API**, for everything declarative — creating and editing networks,
  fleets, servers and templates means writing the custom resources, exactly as `kubectl`
  or GitOps would;
- the **Nautilus control plane** (ConnectRPC), for everything live — server state,
  players, consoles, commands.

The web UI is optional (installed by default). Nothing in the core depends on it:
everything it does can be done with `kubectl` or GitOps.

**Accounts:** [better-auth](https://www.better-auth.com/), with drizzle on
[postgres.js](https://github.com/porsager/postgres), storing users and sessions in
PostgreSQL — by default a CloudNativePG cluster installed with it, or any external
PostgreSQL. The web UI has its own users and roles.

**Authorization:** the web UI calls Kubernetes with its own service account, whose RBAC is
limited to Nautilus resources. It must check the signed-in user's role before every call —
its service account is powerful, which makes the web UI a high-value target.

## Data services

Many plugins need a database — permissions (LuckPerms), economy, statistics — most of them
MySQL/MariaDB, some PostgreSQL or Redis-compatible. Nautilus provides these as **data
services** that admins create in the web UI (or as YAML) and bind to servers. Each engine is
optional (installed by default); a data service whose engine is not installed gets a status
condition saying so.

| Engine                      | Run by                           |
|-----------------------------|----------------------------------|
| PostgreSQL                  | CloudNativePG                    |
| MariaDB (MySQL-compatible)  | mariadb-operator                 |
| Valkey (Redis-compatible)   | open (see [Open questions](#open-questions)) |

Proposed model:

- **`MinecraftDataService`** — one database server: engine, version, replicas (HA),
  storage, backups. The operator translates it into the backend operator's resources.
  Shared per namespace; any network may bind to it.
- **Bindings** in a `MinecraftCluster`, fleet or server spec, e.g.
  `dataServices: [{ name: luckperms, service: main-mariadb }]`. For each binding the operator
  creates a dedicated database and user (Valkey: an ACL user restricted to a key prefix) and
  a credentials Secret. A binding on a cluster is shared by all its servers — LuckPerms needs
  one database for the whole network.
- **Into the servers:** the provisioner offers each binding's credentials as placeholders
  (e.g. `${data.luckperms.host}`, `.port`, `.database`, `.username`, `.password`,
  `.jdbcUrl`) for plugin configs from templates. Credentials never end up in templates or
  images.
- **Access:** NetworkPolicies let only bound pods reach a data service.
- **Retention:** removing a binding or deleting a data service never silently drops data;
  deletion of the data must be explicit.
- **Backups** are done by the backend operators (to object storage).

## Networking and security

- Only proxies (to players) and the web UI (to admins, if installed) are exposed outside the
  cluster.
  Servers and data services get internal Services only.
- A per-network NetworkPolicy lets only that network's proxies reach its servers' game port.
  The agent's endpoint is reachable only by what needs it (probes, metrics scraping, the
  control plane, the web UI, agents of the same network).
- Servers run in offline mode behind Velocity *modern forwarding*, using the network's secret.
  A server must never be reachable without forwarding.
- The operator, builder and gateway hold the only broad RBAC. Agent pods need none.
- Server images live only in the internal registry; the project never publishes images that
  contain Minecraft server software.

## Milestones

1. **Foundations** — remove legacy modules, reshape the CRDs to this model, add leader
   election, status conditions, RBAC and a Helm chart with the core (including the
   registry). Optional parts join the chart with their milestones, opt-out from the start.
2. **Images** — base images, Rust provisioner, builder with `MinecraftBuild` (Paper,
   Velocity, Modrinth/Hangar plugins, git templates).
3. **Static network** — `MinecraftCluster` (forwarding secret, NetworkPolicy),
   `MinecraftProxyFleet`, persistent `MinecraftServer`, gateway, Java agent, Paper and
   Velocity plugins with dynamic proxy registration. *A player can join a server through the proxy.*
4. **Dynamic fleets** — `MinecraftServerFleet` with lifecycle-aware scaling, draining and
   rollouts; ConfigMap runtime layers; OCI templates.
5. **Developer API** — NATS, player lookup, send player, allocation, channel messaging,
   events.
6. **Web UI** — accounts (better-auth), manage networks, fleets, servers and templates; live
   state, consoles.
7. **Data services** — `MinecraftDataService` for PostgreSQL, MariaDB and Valkey, bindings,
   credentials placeholders, creation from the web UI.
8. **Later** — world backups, template write-back, MOTD/maintenance on the proxy, more
   platforms, CLI, extension points for the ecosystem (designed once the base works).

## Current state

Where the code stands relative to the target (last reviewed 2026-09-29):

| Area                            | State                                                                                                      |
|---------------------------------|------------------------------------------------------------------------------------------------------------|
| `lib/crds`                      | Old model: `MinecraftCluster`, `MinecraftServer` (replicas + persistence toggle), `MinecraftProxy`, empty `MinecraftTemplate`. Rust → YAML → Java generation works. |
| `operator` — servers            | Runs itzg images via Deployment + headless Service. Persistence (StatefulSet) is broken: no PVC claim. No status. |
| `operator` — cluster            | Tracks member servers in status, runs a Squid download cache per namespace (to be removed: it cannot cache HTTPS). |
| `operator` — proxies            | Not implemented.                                                                                           |
| `operator` — stopped collector  | Deletes stopped pods of ephemeral servers; replaced by the lifecycle once fleets exist.                     |
| `provisioner`                   | Kotlin/JGit init container that clones git templates. To be rewritten in Rust with the new role.           |
| `builder`                       | Stub (Kotlin, Jib and JGit dependencies present).                                                          |
| `agent`                         | Empty plugin shells (Paper, Velocity, BungeeCord). No Java agent yet.                                      |
| web UI                          | Not started.                                                                                               |
| gateway, protocol               | Not started. `proto/` holds a gRPC placeholder (grpc-java/grpc-kotlin); to move to ConnectRPC with buf.    |
| Legacy, to be removed           | `agent:bungee`, `lib:k8s`, `lib:app-commons`, `lib:api-client`.                                            |

## Open questions

- **Registry exposure:** how the bundled registry is reachable from the nodes (e.g. hostPort
  plus a containerd mirror config, or NodePort/Ingress with TLS), and which registry to
  bundle (e.g. zot).
- **Agent API:** who may call it (operator, other agents, admins, external tools), and how
  callers are authenticated.
- **NATS authentication:** how agents authenticate to NATS without shared secrets — e.g.
  NATS auth callout handled by the gateway, validating the same ServiceAccount tokens.
- **Web UI → Kubernetes authorization:** should the web UI impersonate a Kubernetes group
  per role (e.g. `nautilus:admin`, `nautilus:viewer`), so Kubernetes RBAC stays the final
  check even if the web UI has a bug?
- **Valkey backend:** a Valkey operator (if a mature one exists) or Nautilus running Valkey
  itself (StatefulSet, optionally with replication)?
- **Data service model:** CRD naming and shape; one shared `MinecraftDataService` with many
  databases (proposed) vs. one server per binding.
- Allocation API shape: an agent/gateway call, or also a `MinecraftServerAllocation` resource?
- Template write-back (save a running server as a new template version, e.g. a map builder)?
- Worlds: plain template content, or their own concept with reset/persist policies?
