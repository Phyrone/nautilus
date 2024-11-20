use k8s_openapi::api::core::v1::ObjectReference;
use kube::CustomResource;
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};

#[derive(CustomResource, Debug, Default, Clone, Deserialize, Serialize, JsonSchema)]
#[kube(
    group = "nautilus.phyrone.de",
    kind = "MinecraftCluster",
    version = "v1alpha1",
    root = "MinecraftClusterV1Alpha1",
    status = "MinecraftClusterV1Alpha1Status",
    namespaced
)]
#[serde(rename_all = "camelCase")]
pub struct MinecraftClusterV1Alpha1Spec {}

#[derive(Debug, Default, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct MinecraftClusterV1Alpha1Status {
    pub proxies: Vec<ObjectReference>,
    pub servers: Vec<ObjectReference>,
}
