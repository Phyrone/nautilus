use crate::crds::shared::{MinecraftResourcesSpec, MinecraftTemplateSpec, ProvisionerSpec};
use k8s_openapi::api::apps::v1::DeploymentStrategy;
use k8s_openapi::api::core::v1::{ObjectReference, PodTemplateSpec};
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
    #[schemars(range(min = -1))]
    pub replicas: Option<i32>,
    pub cluster: ObjectReference,
    pub deployment_strategy: Option<DeploymentStrategy>,

    /// If set to true, the server will be deleted once the pod is stopped.
    ///
    /// This is not recommended for persistent servers.
    ///
    /// Default is true for inpersistent servers and false for persistent servers.
    pub delete_on_stop: Option<bool>,
    /// The base image to use for the server
    /// By default, itzg/minecraft-server is used.
    /// This does not affect the provisioner service.
    pub image: Option<String>,
    //TODO settings (properties etc.)
    //TODO env (additional environment variables)
    //TODO additional volumes, mounts, ports, etc.
    //TODO jvm settings (memory, aikar flags, etc.)
    pub resources: Option<MinecraftResourcesSpec>,

    pub template: Option<MinecraftTemplateSpec>,
    pub install: Option<MinecraftServerInstallsSpec>,
    pub persistence: Option<MinecraftServerPersistence>,

    pub provisioner: Option<ProvisionerSpec>,
    pub pod_overrides: Option<PodTemplateSpec>,
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
        paper: String,
    },
    Purpur {
        /// Install Purpur with the given version or latest.
        ///
        /// Purpur is a fork of Paper (more exact Pufferfish) with additional features and optimizations.
        /// Most significantly, it makes a lot more mechanics configurable.
        /// It also add some nice commands like (/tpsbar, /rambar and more).
        ///
        /// Find more information at [PurpurMC](https://purpurmc.org/).
        purpur: String,
    },
    Folia {
        /// Install Folia with the given version or latest.
        ///
        /// Folia is a fork of Paper with regionized multithreading.
        /// WARNING: all plugins need to have explicit support for Folia to work.
        /// Folia makes most sense for large servers with many spread out players.
        ///
        /// Find more information at [PaperMC](https://papermc.io/software/folia).
        folia: String,
    },
    //TODO custom server url
}
#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct MinecraftServerPersistence {
    /// Enable persistence for the server. Data will be stored in a persistent volume.
    /// The server will be handled as stateful set instead of deployment.
    ///
    /// If not set, the default is false.
    ///
    /// More info: https://kubernetes.io/docs/concepts/storage/persistent-volumes/
    pub enabled: Option<bool>,
    /// The storage class of the persistent volume claim.
    ///
    /// If not set, the default is used. (handled by k8s)
    ///
    /// More info: https://kubernetes.io/docs/concepts/storage/persistent-volumes/#class-1
    pub storage_class: Option<String>,
    /// The size of the persistent volume claim.
    ///
    /// If not set, the default is 1Gi.
    /// The storage class of the persistent volume claim.
    pub size: Option<String>,
    /// The access mode of the persistent volume claim.
    ///
    /// If not set, the default is ReadWriteOnce.
    ///
    /// ReadWriteOnce: The volume can be mounted as read-write by a single node.
    /// ReadOnlyMany: The volume can be mounted read-only by many nodes.
    /// ReadWriteMany: The volume can be mounted as read-write by many nodes.
    /// ReadWriteOncePod: The volume can be mounted as read-write by a single pod.
    ///
    /// More info: https://kubernetes.io/docs/concepts/storage/persistent-volumes/#access-modes
    pub access_mode: Option<String>,
}
#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
pub struct MinecraftServerV1Alpha1Status {
    #[serde(default)]
    pub replicas: i32,
}
