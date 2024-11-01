use crate::consts::{FIELD_MANAGER_NAME, IMAGE_NAUTILUS_PROVISIONER, IMAGE_SQUID_PROXY, LABEL_K8S_APP_COMPONENT, LABEL_K8S_APP_NAME, LABEL_NAUTILUS_GROUP, LABEL_NAUTILUS_ON_STOP, LABEL_NAUTILUS_TYPE, SQUID_PROXY_NAME, SQUID_PROXY_PORT, VOLUME_NAME_TEMPLATE};
use crate::utils::{
    bytes_to_jvm_x_limit, parse_memory_spec, parse_memoty_spec_with_relative_support,
};
use k8s_openapi::api::apps::v1::{Deployment, DeploymentSpec};
use k8s_openapi::api::core::v1::{Container, ContainerPort, EnvVar, PodSpec, PodTemplateSpec, ResourceRequirements, Service, ServicePort, ServiceSpec, VolumeMount};
use k8s_openapi::apimachinery::pkg::api::resource::Quantity;
use k8s_openapi::apimachinery::pkg::apis::meta::v1::{LabelSelector, ObjectMeta, OwnerReference};
use kube::api::{Object, Patch, PatchParams};
use kube::runtime::predicates::labels;
use kube::{Api, Client, Resource};
use lib_crds::crds::shared::{GitTemplate, MinecraftResourcesSpec, ProvisionerSpec};
use std::collections::BTreeMap;

pub fn provisioner_init_container(
    provisioner_spec: Option<&ProvisionerSpec>,
    repos: Option<&[GitTemplate]>,
    archives: &mut Vec<String>,
) -> Container {
    const OUTPUT_DIR: &str = "/template";
    let image = provisioner_spec
        .as_ref()
        .and_then(|spec| spec.image.as_deref())
        .unwrap_or(IMAGE_NAUTILUS_PROVISIONER)
        .to_string();

    let mut args = vec!["-o".to_string(), OUTPUT_DIR.to_string()];
    if let Some(templates) = repos {
        args.reserve(4 * templates.len());
        for (i, template) in templates.into_iter().enumerate() {
            let template_name = format!("template-{}", i);
            args.push(template_name.clone());
            args.push("--url".to_string());
            args.push(template.repository.clone());
            if let Some(branch) = &template.branch {
                args.push("--branch".to_string());
                args.push(branch.clone());
            }
            if let Some(path) = &template.path {
                args.push("--path".to_string());
                args.push(path.clone());
            }
            if let Some(paths) = &template.paths {
                for path in paths {
                    args.push("--path".to_string());
                    args.push(path.clone());
                }
            }
            archives.push(format!("{}.zip", template_name));
        }
    }
    Container {
        name: "provisioner".to_string(),
        image: Some(image),
        args: Some(args),
        tty: Some(true),
        stdin: Some(false),
        stdin_once: Some(false),
        volume_mounts: Some(vec![VolumeMount {
            name: VOLUME_NAME_TEMPLATE.to_string(),
            mount_path: OUTPUT_DIR.to_string(),
            ..Default::default()
        }]),
        ..Default::default()
    }
}

pub fn minecraft_resources(
    spec: Option<&MinecraftResourcesSpec>,
    env: &mut Vec<EnvVar>,
    requirements: &mut ResourceRequirements,
) {
    let mem_spec = spec
        .as_ref()
        .and_then(|resources| resources.memory.as_ref());

    if let Some(mem_spec) = mem_spec {
        let max_mem = mem_spec.max_heap.as_ref().and_then(parse_memory_spec);
        let min_mem = mem_spec.min_heap.as_ref().and_then(parse_memory_spec);

        if let Some(max_mem) = max_mem {
            let max_mem_str = bytes_to_jvm_x_limit(max_mem);
            let min_mem = min_mem.unwrap_or(max_mem).min(max_mem);
            if min_mem == max_mem {
                env.push(EnvVar {
                    name: "MEMORY".to_string(),
                    value: Some(max_mem_str),
                    ..Default::default()
                });
            } else {
                let min_mem_str = bytes_to_jvm_x_limit(min_mem);
                env.push(EnvVar {
                    name: "MAX_MEMORY".to_string(),
                    value: Some(max_mem_str),
                    ..Default::default()
                });
                env.push(EnvVar {
                    name: "INIT_MEMORY".to_string(),
                    value: Some(min_mem_str),
                    ..Default::default()
                });
            }
            let jvm_overhead = mem_spec
                .jvm_overhead
                .as_ref()
                .and_then(|v| parse_memoty_spec_with_relative_support(v, max_mem))
                .unwrap_or_else(|| (max_mem as f64 * 0.35).max(256.0) as u64);

            let container_min_mem = min_mem + jvm_overhead;

            let requests = requirements.requests.get_or_insert(BTreeMap::new());
            const MEMORY_RESOURCE: &str = "memory";
            requests.insert(
                MEMORY_RESOURCE.to_string(),
                Quantity(container_min_mem.to_string()),
            );

            if mem_spec.enable_container_limit.unwrap_or(false) {
                let container_limit = max_mem + jvm_overhead;
                let limits = requirements.limits.get_or_insert(BTreeMap::new());
                limits.insert(
                    MEMORY_RESOURCE.to_string(),
                    Quantity(container_limit.to_string()),
                );
            }
        } else {
            env.push(EnvVar {
                name: "MEMORY".to_string(),
                value: Some("".to_string()),
                ..Default::default()
            });
        }
    }
}
