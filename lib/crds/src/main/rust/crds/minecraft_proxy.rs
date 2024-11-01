use k8s_openapi::api::core::v1::{ObjectReference, PodTemplateSpec};
use crate::crds::shared::{MinecraftResourcesSpec, MinecraftTemplateSpec, ProvisionerSpec};
use kube::CustomResource;
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};

#[derive(CustomResource, Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[kube(
    group = "nautilus.phyrone.de",
    kind = "MinecraftProxy",
    version = "v1alpha1",
    root = "MinecraftProxyV1Alpha1",
    status = "MinecraftProxyV1Alpha1Status",
    namespaced
)]
#[serde(rename_all = "camelCase")]
pub struct MinecraftProxyV1Alpha1Spec {
    pub base_image: Option<String>,

    pub resources: Option<MinecraftResourcesSpec>,
    pub template: Option<MinecraftTemplateSpec>,

    pub install: Option<MinecraftProxyInstallSpec>,

    pub provisioner: Option<ProvisionerSpec>,
    pub pod_overrides: Option<PodTemplateSpec>,
}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
pub struct MinecraftProxyInstallSpec {
    pub cluster: ObjectReference,
    pub software: Option<MinecraftProxyInstallSpecServerSoftware>,
    pub spigot: Option<Vec<i32>>,
    pub modrinth: Option<Vec<String>>,
}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(untagged, rename_all = "camelCase")]
pub enum MinecraftProxyInstallSpecServerSoftware {
    BungeeCord { bungeecord: String },
    Waterfall { waterfall: String },
    Velocity { velocity: String },
}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct MinecraftProxyV1Alpha1Status {}
