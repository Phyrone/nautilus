use either::Either;
use kube::CustomResource;
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};

#[derive(CustomResource, Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
#[kube(
    group = "minecraft.phyrone.de",
    version = "v1",
    kind = "Service",
    namespaced
)]
pub struct ServerSpec {

}
