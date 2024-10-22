use kube::CustomResource;
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};

#[derive(CustomResource, Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[kube(
    group = "nautilus.phyrone.de",
    kind = "MinecraftTemplate",
    version = "v1alpha1",
    root = "MinecraftTemplateV1Alpha1",
    status = "MinecraftTemplateV1Alpha1Status",
    namespaced
)]
pub struct MinecraftTemplateV1Alpha1Spec {

}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
pub struct MinecraftTemplateV1Alpha1Status {

}
