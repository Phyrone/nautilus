use k8s_openapi::api::core::v1::PodSpec;
use kube::CustomResource;
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};
use crate::template::TemplateSpec;

/// Define a Minecraft service
/// The nautilus operator will create the necessary resources to run the service.
/// Depeding on configuration, they will be created as pods, deployments or statefulsets.
/// when:
///   - solo = true: 1 pod will be created
///   - persistent = true: services will be created in a statefulset
///   - persistent = false: services will be created in a deployment/replicaset
#[derive(CustomResource, Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
#[kube(
    group = "minecraft.phyrone.de",
    version = "v1",
    kind = "MinecraftService",
    namespaced
)]
pub struct MinecraftServiceSpec {
    pub template: TemplateData,
    pub instances: InstancesSpec,
    pub persistent: Option<PersistencySpec>,

}


#[derive(Debug, Serialize, Deserialize, Clone, JsonSchema)]
#[serde(untagged)]
pub enum TemplateData {
    /// Look up a template by name (implicit)
    RefNamedImplicit(String),
    /// Look up a template by name (explicit)
    RefNamedExplici {
        #[serde(rename = "ref", alias = "reference")]
        reference: String,
    },
    /// Inline template spec
    TemplateSpec {
        spec: TemplateSpec
    },
}
impl Default for TemplateData {
    fn default() -> Self {
        TemplateData::RefNamedImplicit("".to_string())
    }
}

#[derive(Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
pub struct InstancesSpec {
    #[serde(default, alias = "count")]
    pub replicas: Option<u32>,
    /// If true, only one instance will be created [replicas] will be ignored.
    #[serde(default)]
    pub solo: bool,
    //TODO autoscaling
}

#[derive(Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
pub struct PersistencySpec {
    /// If true, a persistent volume claim will be created. 
    /// It also changes to naming from random to sequential except for solo instances.
    pub enabled: bool,
    /// The storage class to use for the persistent volume claim
    /// If not set, the default storage class will be used (let kubernetes decide)
    pub storage_class: Option<String>,
    /// The size of the persistent volume claim
    /// If not set, the default size is 1Gi
    /// Further details depend on the storage class
    pub size: Option<String>,
}
