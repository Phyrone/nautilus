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
    #[serde(flatten)]
    pub platform_specific: Option<ServerTemplatePlatformSpecificSpec>,
}


#[derive(Debug, Serialize, Deserialize, Clone, JsonSchema)]
#[serde(untagged)]
pub enum ServerTemplatePlatformSpecificSpec {
    PaperMC {
        paper: PaperMcServerTemplateSpec
    },
    Velocity {
        velocity: VelocityServerTemplateSpec
    },
}
impl Default for ServerTemplatePlatformSpecificSpec {
    fn default() -> Self {
        ServerTemplatePlatformSpecificSpec::PaperMC {
            paper: Default::default()
        }
    }
}
