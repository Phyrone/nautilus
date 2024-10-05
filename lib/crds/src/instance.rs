use either::Either;
use kube::CustomResource;
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};

#[derive(CustomResource, Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
#[kube(
    group = "minecraft.phyrone.de",
    version = "v1",
    kind = "Server",
    namespaced
)]
pub struct ServerSpec {
    pub image: Option<String>,
    
    #[serde(default)]
    pub jvm: ServerInstanceJvmSpec,
    #[serde(default)]
    pub resource: ServerInstanceResourceSpec,
}

#[derive(Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
pub struct ServerInstanceJvmSpec {
    pub enable_aikar_flags: Option<bool>,
    pub enable_simd: Option<bool>,

    #[serde(default)]
    pub additional_args: Vec<String>,
    
}
#[derive(Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
pub struct ServerInstanceResourceSpec {
    pub memory: ServerInstanceResourceMemorySpec,
}
#[derive(Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
pub struct ServerInstanceResourceMemorySpec {
    pub min : Option<String>,
    pub max : Option<String>,
}
