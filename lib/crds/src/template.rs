use kube::CustomResource;
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};

#[derive(CustomResource, Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
#[kube(
    group = "minecraft.phyrone.de",
    version = "v1alpha1",
    kind = "Template",
    namespaced
)]
pub struct TemplateSpec {
    
}
