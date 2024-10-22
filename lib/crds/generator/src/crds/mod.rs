use k8s_openapi::apiextensions_apiserver::pkg::apis::apiextensions::v1::CustomResourceDefinition;
use kube::core::crd::merge_crds;
use kube::CustomResourceExt;

pub mod minecraft_service;
mod template;

pub fn all() -> Vec<CustomResourceDefinition> {
    let minecraft_service = merge_crds(
        vec![minecraft_service::MinecraftServiceV1Alpha1::crd()],
        "v1alpha1",
    ).expect("Failed to merge MinecraftService CRDs");
    let template = merge_crds(
        vec![template::MinecraftTemplateV1Alpha1::crd()],
        "v1alpha1",
    ).expect("Failed to merge MinecraftTemplate CRDs");


    vec![minecraft_service, template]
}
