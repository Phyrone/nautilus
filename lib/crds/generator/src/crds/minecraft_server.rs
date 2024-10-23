use k8s_openapi::api::core::v1::ObjectReference;
use kube::CustomResource;
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};

#[derive(CustomResource, Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[kube(
    group = "nautilus.phyrone.de",
    kind = "MinecraftServer",
    version = "v1alpha1",
    root = "MinecraftServiceV1Alpha1",
    status = "MinecraftServerV1Alpha1Status",
    namespaced
)]
#[serde(rename_all = "camelCase")]
pub struct MinecraftServerV1Alpha1Spec {
    #[schemars(range(min = 0))]
    replicas: Option<i32>,
    #[serde(flatten)]
    source: MinecraftServerSourceSpec,
    persistence: Option<MinecraftServerPersistence>,
}
#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(untagged)]
pub enum MinecraftServerSourceSpec {
    Image { image: String },
    Template { template: TemplateRef },
}
#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct TemplateRef {
    #[serde(rename = "ref")]
    reference: ObjectReference,
}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct MinecraftServerPersistence {
    enabled: Option<bool>,
    storage_class: Option<String>,
    size: Option<String>,
}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
pub struct MinecraftServerV1Alpha1Status {}
