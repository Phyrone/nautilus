use k8s_openapi::apiextensions_apiserver::pkg::apis::apiextensions::v1::CustomResourceDefinition;
use kube::core::crd::merge_crds;
use kube::CustomResourceExt;

pub mod minecraft_server;
mod template;


const PRIMARY_VERSION: &str = "v1alpha1";
pub fn all() -> Vec<CustomResourceDefinition> {


    let minecraft_service = merge_crds(
        vec![minecraft_server::MinecraftServiceV1Alpha1::crd()],
        PRIMARY_VERSION,
    ).expect("Failed to merge MinecraftService CRDs");
    let template = merge_crds(
        vec![template::MinecraftTemplateV1Alpha1::crd()],
        PRIMARY_VERSION,
    ).expect("Failed to merge MinecraftTemplate CRDs");


    vec![ minecraft_service, template]
}
