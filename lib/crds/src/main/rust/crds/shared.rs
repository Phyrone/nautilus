use k8s_openapi::api::core::v1::ObjectReference;
use k8s_openapi::apimachinery::pkg::util::intstr::IntOrString;
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
pub enum MinecraftTemplateSpec {
    /// NOT IMPLEMENTED YET
    #[serde(rename = "ref")]
    Ref(ObjectReference),

    /// Use a git repository as the template.
    ///
    /// The repository will be checked out at every server start.
    /// Fast-forward will be used if possible.
    /// Shallow clone is used if not cloned before.
    #[serde(rename = "git")]
    Git(Vec<GitTemplate>),
}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct GitTemplate {
    /// The url to the git repository to check out.
    ///
    /// This can be any valid git url implemented in JGit (which should be equal to original git).
    pub repository: String,
    /// The branch to check out. If not set, the default branch is used.
    pub branch: Option<String>,

    /// The path inside the repository to use as the template.
    pub path: Option<String>,

    pub paths: Option<Vec<String>>,
    //TODO add credentials
}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct MinecraftResourcesSpec {
    pub memory: Option<MinecraftResourcesSpecMemory>,
    //TODO cpu settings
}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
#[serde(rename_all = "camelCase")]
pub struct MinecraftResourcesSpecMemory {
    /// The max heap size for the JVM.
    /// This is the amount of memory the JVM can use for the heap.
    /// The JVM will use more memory than this value. The exact amount depends on the JVM implementation.
    ///
    /// The value can be a number in bytes or a string with a unit.
    /// An empty string means no limit.
    ///
    /// Default is 2G.
    pub max_heap: Option<IntOrString>,

    /// The min heap size for the JVM.
    /// This is the amount of memory the JVM will start with.
    /// The JVM will use more memory than this value. The exact amount depends on the JVM implementation.
    ///
    /// The value can be a number in bytes or a string with a unit.
    /// An empty string means no limit.
    ///
    /// Default is the same as max heap size. (recommended)
    ///
    /// This also sets the requested memory for the container.
    pub min_heap: Option<IntOrString>,

    /// A margin to add to the max heap size for the JVM overhead like metaspace.
    ///
    /// Default is ca. 35% of the max heap size.
    /// If unsure, leave it at the default.
    pub jvm_overhead: Option<IntOrString>,

    /// If set to true, the container memory limit will be set to the max heap size + jvm overhead.
    /// If set to false, memory limtis are only enfored via heap size.
    /// Regardess of this setting min memory is always requested to k8s.
    ///
    /// Default is false.
    pub enable_container_limit: Option<bool>,
}

#[derive(Debug, Clone, Deserialize, Serialize, JsonSchema)]
pub struct ProvisionerSpec {
    pub enabled: Option<bool>,
    pub image: Option<String>,
}

