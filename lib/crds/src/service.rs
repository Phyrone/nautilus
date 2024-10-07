use crate::template::MinecraftTemplateV1a1Spec;
use k8s_openapi::api::core::v1::PodSpec;
use kube::CustomResource;
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};

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
    version = "v1apha1",
    kind = "MinecraftService",
    namespaced
)]
pub struct MinecraftServiceV1a1Spec {
    pub template: TemplateData,
    pub instances: ReplicaInstacesSpec,
    pub persistent: Option<PersistencySpec>,
}

#[derive(Debug, Serialize, Deserialize, Clone, JsonSchema)]
#[serde(untagged)]
//take single
pub enum InstancesSpec {
    Singleton(SingletonInstanceSpec),
    Replicas(ReplicaInstacesSpec),
}
impl Default for InstancesSpec {
    fn default() -> Self {
        InstancesSpec::Singleton(SingletonInstanceSpec::default())
    }
}

#[derive(Debug, Serialize, Deserialize, Clone, JsonSchema)]
#[serde(untagged)]
pub enum TemplateData {
    /// Look up a template by name (explicit)
    Ref {
        #[serde(rename = "ref", alias = "reference")]
        reference: String,
    },
    /// Inline template spec
    TemplateSpec { spec: MinecraftTemplateV1a1Spec },
}
impl Default for TemplateData {
    fn default() -> Self {
        TemplateData::Ref {
            reference: "".to_string(),
        }
    }
}


#[derive(Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
pub struct ReplicaInstacesSpec {
    #[serde(default, alias = "count")]
    pub replicas: u32,
}
#[derive(Debug, Default, Serialize, Deserialize, Clone, JsonSchema)]
pub struct SingletonInstanceSpec {
    pub singleton: bool,
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
