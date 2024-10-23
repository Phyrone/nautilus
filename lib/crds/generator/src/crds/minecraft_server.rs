use k8s_openapi::api::apps::v1::DeploymentStrategy;
use k8s_openapi::api::core::v1::ObjectReference;
use k8s_openapi::apimachinery::pkg::util::intstr::IntOrString;
use kube::CustomResource;
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};

#[derive(CustomResource, Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[kube(
    group = "nautilus.phyrone.de",
    kind = "MinecraftServer",
    version = "v1alpha1",
    root = "MinecraftServerV1Alpha1",
    status = "MinecraftServerV1Alpha1Status",
    namespaced
)]
#[serde(rename_all = "camelCase")]
pub struct MinecraftServerV1Alpha1Spec {
    #[schemars(range(min = 0))]
    replicas: Option<i32>,
    deployment_strategy: Option<DeploymentStrategy>,
    /// The base image to use for the server
    /// By default, itzg/minecraft-server is used.
    /// This does not affect the provisioner service.
    image: Option<String>,
    //TODO settings (properties etc.)
    //TODO env (additional environment variables)
    //TODO additional volumes, mounts, ports, etc.
    //TODO jvm settings (memory, aikar flags, etc.)
    resources: Option<MinecraftServerResourcesSpec>,

    template: Option<MinecraftTemplateSpec>,
    install: Option<MinecraftServerInstallsSpec>,
    persistence: Option<MinecraftServerPersistence>,
}
#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
pub enum MinecraftTemplateSpec {
    /// NOT IMPLEMENTED YET
    #[serde(rename = "ref")]
    Ref(ObjectReference),

    /// Use a git repository as the template.
    ///
    /// The repository will be checked out at every server start.
    /// Fast-forward will be used if possible.
    /// Shallow clone is used if not cloned before.
    #[serde(rename = "git")]
    Git(GitTemplate),
}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct GitTemplate {
    /// The url to the git repository to check out.
    ///
    /// This can be any valid git url implemented in JGit (which should be equal to original git).
    repository: String,
    /// The branch to check out. If not set, the default branch is used.
    branch: Option<String>,
    /// A subdirectory which contains the files.
    path: Option<String>,
}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct MinecraftServerResourcesSpec {
    pub memory: Option<MineecraftServerResourcesMemorySpec>,

    //TODO cpu settings
}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct MineecraftServerResourcesMemorySpec {
    /// The max heap size for the JVM.
    /// This is the amount of memory the JVM can use for the heap.
    /// The JVM will use more memory than this value. The exact amount depends on the JVM implementation.
    ///
    /// The value can be a number in bytes or a string with a unit.
    /// An empty string means no limit.
    ///
    /// Default is 2G.
    pub max_heap: Option<IntOrString>,

    /// The min heap size for the JVM.
    /// This is the amount of memory the JVM will start with.
    /// The JVM will use more memory than this value. The exact amount depends on the JVM implementation.
    ///
    /// The value can be a number in bytes or a string with a unit.
    /// An empty string means no limit.
    ///
    /// Default is the same as max heap size. (recommended)
    ///
    /// This also sets the requested memory for the container.
    pub min_heap: Option<IntOrString>,

    /// A margin to add to the max heap size for the JVM overhead like metaspace.
    ///
    /// Default is 25% of the max heap size.
    /// If unsure, leave it at the default.
    pub jvm_overhead: Option<IntOrString>,

    /// If set to true, the container memory limit will be set to the max heap size + jvm overhead.
    /// If set to false, no memory limit will be set but only the heap size.
    ///
    /// Default is true.
    pub enable_container_limit: Option<bool>,
}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct MinecraftServerInstallsSpec {
    pub software: Option<MinecraftServerInstallSpecServerSoftware>,
    //TODO install datapacks
    //TODO install resourcepack
    //TODO install world
    //TODO install mods?

    /// A list of plugins to install.
    ///
    /// These are the resource ids inside spigotmc.
    /// They will be automatically placed in the plugins folder.
    /// Not all resources are downloadable as some are premium or point to external sites which might not be supported.
    /// The resource id is part of the url of the resource. f.e. `https://www.spigotmc.org/resources/luckperms.28140/` has the id `28140`.
    ///
    /// See more at [Itzg's Minecraft Docker Image](https://docker-minecraft-server.readthedocs.io/en/latest/mods-and-plugins/spiget/)
    /// We might extract the resource id from the url ourselves in the future.
    pub spigot: Option<Vec<i32>>,
    /// A list of modrinth resources to install.
    ///
    /// This can be plugins, mods, datapacks, etc.
    /// Thanks to the work of itzg they will be automatically placed in the correct folder.
    /// Resources are identified by their slug or project id.
    /// Datapacks are prefixed with `datapack:`.
    /// See more at [Itzg's Minecraft Docker Image](https://docker-minecraft-server.readthedocs.io/en/latest/mods-and-plugins/modrinth/)
    pub modrinth: Option<Vec<String>>,
}
#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(rename_all = "camelCase", untagged)]
pub enum MinecraftServerInstallSpecServerSoftware {
    Paper {
        /// Install PaperMC with the given version or latest.
        ///
        /// Papermc is an optimized minecraft server based on Bukkit/Spigot.
        /// It implements the Bukkit plugin api and has a lot of fancy built-in features.
        /// Find more information at [PaperMC](https://papermc.io/software/paper).
        paper: String
    },
    Purpur {
        /// Install Purpur with the given version or latest.
        ///
        /// Purpur is a fork of Paper (more exact Pufferfish) with additional features and optimizations.
        /// Most significantly, it makes a lot more mechanics configurable.
        /// It also add some nice commands like (/tpsbar, /rambar and more).
        ///
        /// Find more information at [PurpurMC](https://purpurmc.org/).
        purpur: String
    },
    Folia {
        /// Install Folia with the given version or latest.
        ///
        /// Folia is a fork of Paper with regionized multithreading.
        /// WARNING: all plugins need to have explicit support for Folia to work.
        /// Folia makes most sense for large servers with many spread out players.
        /// Find more information at [PaperMC](https://papermc.io/software/folia).
        folia: String
    },
    //TODO custom server url
}
#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct MinecraftServerPersistence {
    /// Enable persistence for the server.
    /// Data will be stored in a persistent volume.
    /// The server will be handled as stateful set instead of deployment.
    enabled: Option<bool>,
    storage_class: Option<String>,
    size: Option<String>,
}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
pub struct MinecraftServerV1Alpha1Status {}
