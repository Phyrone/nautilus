use k8s_openapi::api::core::v1::PodSpec;
use kube::CustomResource;
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};

#[derive(CustomResource, Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
#[kube(
    group = "minecraft.phyrone.de",
    version = "v1alpha1",
    kind = "MinecraftTemplate",
    namespaced
)]
pub struct MinecraftTemplateV1a1Spec {
    /// Base image to use for the template
    pub image: Option<String>,
    /// Pull a template from the given source
    pub source: Option<SourceRef>,
    /// Build in install utilities
    pub install: Option<InstallSpec>,
}

#[derive(Debug, Serialize, Deserialize, Clone, JsonSchema)]
#[serde(untagged)]
//TODO open up for any kind of source
pub enum SourceRef {
    Git { git: GitSource },
    Artifact { artifact: ArtifactSource },
}

#[derive(Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
pub struct GitSource {
    pub url: String,
}

#[derive(Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
pub struct ArtifactSource {
    pub url: String,
}

#[derive(Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
pub struct InstallSpec {
    #[serde(flatten)]
    pub software: Option<PlatformInstallSpec>,

    #[serde(flatten)]
    pub plugins: Option<InstallPluginsSpec>,
}
#[derive(Debug, Serialize, Deserialize, Clone, JsonSchema)]
#[serde(untagged)]
pub enum PlatformInstallSpec {
    Paper { paper: PaperInstallSpec },
    Velocity { velocity: VelocityInstallSpec },
    //TODO forge, fabric, folia, bungeecord
}

#[derive(Debug, Serialize, Deserialize, Clone, JsonSchema)]
enum PaperFlavor {
    Paper,
    Folia,
    Purpur,
}

fn latest() -> String {
    "latest".to_string()
}

#[derive(Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
pub struct PaperInstallSpec {
    //default to latest
    #[serde(default = "latest")]
    pub version: String,
    pub flavor: Option<PaperFlavor>,
}

#[derive(Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
pub struct VelocityInstallSpec {
    #[serde(default = "latest")]
    pub version: String,
}

#[derive(Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
pub struct InstallPluginsSpec {
    /// A list of plugin urls to install
    /// Urls will be parsed in best effort.
    /// supports urls from:
    ///  - spigotmc.org
    ///  - modrinth.com
    ///  - direct download links
    pub plugins: Vec<String>,
}
