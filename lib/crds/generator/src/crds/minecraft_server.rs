use k8s_openapi::api::core::v1::ObjectReference;
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
    /// The base image to use for the server
    /// By default, itzg/minecraft-server is used.
    /// This does not affect the provisioner service.
    image: Option<String>,
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
pub struct MinecraftServerInstallsSpec {
    pub software: Option<MinecraftServerInstallSpecServerSoftware>,
    pub plugins: Option<Vec<String>>,
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
