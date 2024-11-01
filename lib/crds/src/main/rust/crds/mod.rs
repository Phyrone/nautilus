use k8s_openapi::apiextensions_apiserver::pkg::apis::apiextensions::v1::CustomResourceDefinition;
use kube::core::crd::merge_crds;
use kube::CustomResourceExt;

pub mod minecraft_cluster;
pub mod minecraft_proxy;
pub mod minecraft_server;
pub mod shared;
pub mod template;

const PRIMARY_VERSION: &str = "v1alpha1";
pub fn all() -> Vec<CustomResourceDefinition> {
    let minecraft_cluster = merge_crds(
        vec![minecraft_cluster::MinecraftClusterV1Alpha1::crd()],
        PRIMARY_VERSION,
    )
    .expect("Failed to merge MinecraftCluster CRDs");

    let minecraft_service = merge_crds(
        vec![minecraft_server::MinecraftServerV1Alpha1::crd()],
        PRIMARY_VERSION,
    )
    .expect("Failed to merge MinecraftService CRDs");
    let minecraft_proxy = merge_crds(
        vec![minecraft_proxy::MinecraftProxyV1Alpha1::crd()],
        PRIMARY_VERSION,
    )
    .expect("Failed to merge MinecraftProxy CRDs");

    let template = merge_crds(
        vec![template::MinecraftTemplateV1Alpha1::crd()],
        PRIMARY_VERSION,
    )
    .expect("Failed to merge MinecraftTemplate CRDs");

    vec![
        minecraft_cluster,
        minecraft_service,
        minecraft_proxy,
        template,
    ]
}
