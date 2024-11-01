use crate::consts::{FIELD_MANAGER_NAME, IMAGE_MINECRAFT_SERVER, LABEL_NAUTILUS_GROUP, LABEL_NAUTILUS_ON_STOP, LABEL_NAUTILUS_TYPE, LABEL_VALUE_NAUTILUS_ON_STOP_DELETE, LABEL_VALUE_NAUTILUS_ON_STOP_KEEP, LABEL_VALUE_NAUTILUS_TYPE_SERVER, SQUID_PROXY_NAME, SQUID_PROXY_PORT, VOLUME_NAME_MINECRAFT_SERVER, VOLUME_NAME_TEMPLATE};
use crate::reconciler::shared::{minecraft_resources, provisioner_init_container};
use crate::reconciler::{
    Context, RunReconcilerError, DEFAULT_RECONCILE_INTERVAL, ERROR_RECONCILE_INTERVAL,
};
use futures::{StreamExt, TryStreamExt};
use k8s_openapi::api::apps::v1::{
    Deployment, DeploymentSpec, RollingUpdateStatefulSetStrategy, StatefulSet, StatefulSetOrdinals,
    StatefulSetSpec, StatefulSetUpdateStrategy,
};
use k8s_openapi::api::core::v1::{
    Container, ContainerPort, EnvVar, EnvVarSource, ExecAction, ObjectFieldSelector,
    PersistentVolumeClaimVolumeSource, PodSpec, PodTemplateSpec, Probe, ResourceRequirements,
    Service, ServicePort, ServiceSpec, Volume, VolumeMount,
};
use k8s_openapi::apimachinery::pkg::apis::meta::v1::{LabelSelector, ObjectMeta, OwnerReference};
use k8s_openapi::DeepMerge;
use kube::api::{DeleteParams, Patch, PatchParams};
use kube::runtime::controller::Action;
use kube::runtime::reflector::ObjectRef;
use kube::runtime::Controller;
use kube::{Api, Client, Resource, ResourceExt};
use lib_crds::crds::minecraft_cluster::MinecraftClusterV1Alpha1;
use lib_crds::crds::minecraft_server::{
    MinecraftServerInstallSpecServerSoftware, MinecraftServerV1Alpha1,
};
use lib_crds::crds::shared::MinecraftTemplateSpec;
use std::collections::BTreeMap;
use std::sync::Arc;
use thiserror::Error;
use tokio::time::Instant;
use tokio::{pin, try_join};
use tokio_util::sync::CancellationToken;
use tracing::{info, warn};

pub async fn run(
    ctx: Arc<Context>,
    shutdown: CancellationToken,
) -> error_stack::Result<(), RunReconcilerError> {
    let minecraft_server_point = Api::<MinecraftServerV1Alpha1>::all(ctx.client.clone());
    let net_services = Api::<Service>::all(ctx.client.clone());
    let deployments = Api::<Deployment>::all(ctx.client.clone());
    let stateful_sets = Api::<StatefulSet>::all(ctx.client.clone());
    let minecraft_clusters = Api::<MinecraftClusterV1Alpha1>::all(ctx.client.clone());

    let config = kube::runtime::watcher::Config::default();

    let reconcile_stream = Controller::new(minecraft_server_point, config.clone())
        .graceful_shutdown_on(shutdown.cancelled_owned())
        .owns(net_services, config.clone())
        .owns(deployments, config.clone())
        .owns(stateful_sets, config.clone())
        .watches(minecraft_clusters, config.clone(), map_minecraft_cluster)
        .run(reconcile, reconcile_error, ctx);

    pin!(reconcile_stream);

    while let Some(_event) = reconcile_stream.next().await {
        //TODO handle event
    }

    Ok(())
}

#[derive(Error, Debug)]
pub enum ReconcileError {
    #[error("kube client failed: {0}")]
    KubeClient(#[from] kube::Error),
}

fn map_minecraft_cluster(
    cluster: MinecraftClusterV1Alpha1,
) -> Vec<ObjectRef<MinecraftServerV1Alpha1>> {
    cluster
        .status
        .map(|status| status.servers)
        .map(|servers| {
            servers
                .into_iter()
                .filter_map(|server| {
                    let name = server.name?;
                    let namespace = server.namespace?;
                    Some(ObjectRef::new(&name).within(&namespace))
                })
                .collect()
        })
        .unwrap_or_default()
}

fn reconcile_error(
    server: Arc<MinecraftServerV1Alpha1>,
    error: &ReconcileError,
    ctx: Arc<Context>,
) -> Action {
    warn!("Reconcile error: {}", error);
    Action::requeue(ERROR_RECONCILE_INTERVAL)
}

async fn reconcile(
    mut minecraft_server: Arc<MinecraftServerV1Alpha1>,
    ctx: Arc<Context>,
) -> Result<Action, ReconcileError> {
    let time = Instant::now();
    let client = &ctx.client;
    let name = minecraft_server.name_unchecked();
    let namespace = minecraft_server
        .namespace()
        .expect(".metadata.namespace must be set");

    let cluster = owning_cluster(&namespace, client, &mut minecraft_server).await?;
    if let Some(cluster) = cluster {
        let owner_reference = minecraft_server
            .controller_owner_ref(&())
            .expect("owner reference should have been created");

        let persistent = minecraft_server
            .spec
            .persistence
            .as_ref()
            .map(|p| p.enabled.unwrap_or(false))
            .unwrap_or(false);

        try_join!(
            reconcile_service(
                &name,
                &namespace,
                owner_reference.clone(),
                &minecraft_server,
                &ctx
            ),
            reconcile_deployment(
                &name,
                &namespace,
                owner_reference.clone(),
                &minecraft_server,
                &ctx,
                persistent
            ),
            reconcile_stateful_set(
                &name,
                &namespace,
                owner_reference.clone(),
                &minecraft_server,
                &ctx,
                !persistent
            ),
        )?;
        let time = time.elapsed();

        info!("Reconciled minecraft server {} ({:?})", name, time);
    } else {
        warn!(
            "Minecraft server {} does not point to a cluster, skip",
            name
        );
    }

    Ok(Action::requeue(DEFAULT_RECONCILE_INTERVAL))
}

async fn owning_cluster(
    namespace: &str,
    client: &Client,
    minecraft_server: &mut Arc<MinecraftServerV1Alpha1>,
) -> Result<Option<MinecraftClusterV1Alpha1>, ReconcileError> {
    let owning_cluster_ref = &minecraft_server.spec.cluster;
    let cluster_namespace = owning_cluster_ref.namespace.as_deref().unwrap_or(namespace);
    let cluster_name = match &owning_cluster_ref.name {
        None => return Ok(None),
        Some(name) => name,
    };

    if cluster_namespace != namespace {
        return Ok(None);
    }
    let clusters = Api::<MinecraftClusterV1Alpha1>::namespaced(client.clone(), namespace);
    let owning_cluster = clusters
        .get_opt(cluster_name)
        .await
        .map_err(ReconcileError::KubeClient)?;
    let owning_cluster = match owning_cluster {
        Some(cluster) => cluster,
        None => return Ok(None),
    };
    Ok(Some(owning_cluster))
}

async fn reconcile_service(
    name: &str,
    namespace: &str,
    owner: OwnerReference,
    minecraft_server: &MinecraftServerV1Alpha1,
    ctx: &Context,
) -> Result<(), ReconcileError> {
    let client = &ctx.client;
    let service_name = format!("mc-{}", name);
    let services_endpoint = Api::<Service>::namespaced(client.clone(), &namespace);
    let mut selector = BTreeMap::new();
    selector.insert(LABEL_NAUTILUS_GROUP.to_string(), name.to_string());
    selector.insert(
        LABEL_NAUTILUS_TYPE.to_string(),
        LABEL_VALUE_NAUTILUS_TYPE_SERVER.to_string(),
    );

    let service = Service {
        metadata: ObjectMeta {
            name: Some(service_name.clone()),
            namespace: Some(namespace.to_string()),
            owner_references: Some(vec![owner]),
            ..Default::default()
        },
        spec: Some(ServiceSpec {
            type_: Some("ClusterIP".to_string()),
            cluster_ip: Some("None".to_string()),
            ports: Some(vec![ServicePort {
                name: Some("minecraft".to_string()),
                port: 25565,
                protocol: Some("TCP".to_string()),
                ..Default::default()
            }]),
            selector: Some(select_pod_labels(name)),
            ip_family_policy: Some("PreferDualStack".to_string()),
            publish_not_ready_addresses: Some(true),
            session_affinity: None,
            ..Default::default()
        }),
        ..Default::default()
    };

    let params = PatchParams::apply(FIELD_MANAGER_NAME).force();
    let res = services_endpoint
        .patch(&service_name, &params, &Patch::Apply(service))
        .await
        .map_err(ReconcileError::KubeClient);
    if let Err(e) = res {
        services_endpoint
            .delete(&service_name, &DeleteParams::foreground())
            .await
            .map_err(ReconcileError::KubeClient)?;
        return Err(e);
    }

    Ok(())
}

async fn reconcile_deployment(
    name: &str,
    namespace: &str,
    owner: OwnerReference,
    minecraft_server: &MinecraftServerV1Alpha1,
    ctx: &Context,
    delete: bool,
) -> Result<(), ReconcileError> {
    let deployments_endpoint = Api::<Deployment>::namespaced(ctx.client.clone(), &namespace);
    if delete {
        let exists = deployments_endpoint
            .get_opt(&name)
            .await
            .map_err(ReconcileError::KubeClient)?
            .is_some();
        if exists {
            deployments_endpoint
                .delete(&name, &DeleteParams::foreground())
                .await
                .map_err(ReconcileError::KubeClient)?;
        }
    } else {
        let replicas = minecraft_server.spec.replicas.unwrap_or(1);
        let deployment = Deployment {
            metadata: ObjectMeta {
                name: Some(name.to_string()),
                namespace: Some(namespace.to_string()),
                owner_references: Some(vec![owner]),
                ..Default::default()
            },
            spec: Some(DeploymentSpec {
                template: pod_template(name, namespace, minecraft_server, false),
                replicas: Some(replicas),
                strategy: minecraft_server.spec.deployment_strategy.clone(),
                selector: LabelSelector {
                    match_labels: Some(select_pod_labels(name)),
                    ..Default::default()
                },
                ..Default::default()
            }),
            ..Default::default()
        };

        deployments_endpoint
            .patch(
                &name,
                &PatchParams::apply(FIELD_MANAGER_NAME).force(),
                &Patch::Apply(deployment),
            )
            .await
            .map_err(ReconcileError::KubeClient)?;
    }

    Ok(())
}

async fn reconcile_stateful_set(
    name: &str,
    namespace: &str,
    owner: OwnerReference,
    minecraft_server: &MinecraftServerV1Alpha1,
    ctx: &Context,
    delete: bool,
) -> Result<(), ReconcileError> {
    let statefulsets_endpoint = Api::<StatefulSet>::namespaced(ctx.client.clone(), namespace);

    if delete {
        let exists = statefulsets_endpoint
            .get_opt(name)
            .await
            .map_err(ReconcileError::KubeClient)?
            .is_some();
        if exists {
            info!("Deleting statefulset {}", name);
            let time = Instant::now();
            statefulsets_endpoint
                .delete(name, &DeleteParams::foreground())
                .await
                .map_err(ReconcileError::KubeClient)?;
            let time = time.elapsed();
            info!("Deleted statefulset {} ({:?})", name, time);
        }
    } else {
        let strategy = minecraft_server
            .spec
            .deployment_strategy
            .as_ref()
            .map(|strategy| StatefulSetUpdateStrategy {
                type_: strategy.type_.clone(),
                rolling_update: strategy.rolling_update.clone().map(|r| {
                    RollingUpdateStatefulSetStrategy {
                        max_unavailable: r.max_unavailable,
                        ..Default::default()
                    }
                }),
            });
        let statefultset = StatefulSet {
            metadata: ObjectMeta {
                name: Some(name.to_string()),
                namespace: Some(namespace.to_string()),
                owner_references: Some(vec![owner]),
                ..Default::default()
            },
            spec: Some(StatefulSetSpec {
                replicas: Some(minecraft_server.spec.replicas.unwrap_or(1)),
                update_strategy: strategy,
                selector: LabelSelector {
                    match_labels: Some(select_pod_labels(&name)),
                    ..Default::default()
                },
                template: pod_template(name, namespace, minecraft_server, true),
                ordinals: Some(StatefulSetOrdinals { start: Some(1) }),
                ..Default::default()
            }),
            ..Default::default()
        };

        statefulsets_endpoint
            .patch(
                name,
                &PatchParams::apply(FIELD_MANAGER_NAME).force(),
                &Patch::Apply(statefultset),
            )
            .await
            .map_err(ReconcileError::KubeClient)?;
    }
    Ok(())
}

fn select_pod_labels(name: &str) -> BTreeMap<String, String> {
    let mut labels = BTreeMap::new();
    labels.insert(LABEL_NAUTILUS_GROUP.to_string(), name.to_string());
    labels.insert(
        LABEL_NAUTILUS_TYPE.to_string(),
        LABEL_VALUE_NAUTILUS_TYPE_SERVER.to_string(),
    );
    labels
}

fn pod_template(
    name: &str,
    namespace: &str,
    minecraft_server: &MinecraftServerV1Alpha1,
    persistent: bool,
) -> PodTemplateSpec {
    let mut archives = Vec::new();
    let provisioner = server_provisioner_init_container(minecraft_server, &mut archives);

    let volumes = vec![
        Volume {
            name: VOLUME_NAME_TEMPLATE.to_string(),
            empty_dir: Some(Default::default()),
            ..Default::default()
        },
        if persistent {
            Volume {
                name: VOLUME_NAME_MINECRAFT_SERVER.to_string(),
                persistent_volume_claim: Some(PersistentVolumeClaimVolumeSource {
                    read_only: Some(false),
                    ..Default::default()
                }),
                ..Default::default()
            }
        } else {
            Volume {
                name: VOLUME_NAME_MINECRAFT_SERVER.to_string(),
                empty_dir: Some(Default::default()),
                ..Default::default()
            }
        },
    ];
    let mut labels = select_pod_labels(name);
    let delete_on_stop = minecraft_server.spec.delete_on_stop.unwrap_or(!persistent);
    labels.insert(
        LABEL_NAUTILUS_ON_STOP.to_string(),
        if delete_on_stop {
            LABEL_VALUE_NAUTILUS_ON_STOP_DELETE
        } else {
            LABEL_VALUE_NAUTILUS_ON_STOP_KEEP
        }
            .to_string(),
    );
    let mut pod_template = PodTemplateSpec {
        metadata: Some(ObjectMeta {
            labels: Some(labels),
            ..Default::default()
        }),
        spec: Some(PodSpec {
            init_containers: Some(vec![provisioner]),
            containers: vec![minecraft_container(
                name,
                namespace,
                minecraft_server,
                &archives,
            )],
            //TODO make termination grace period configurable
            termination_grace_period_seconds: Some(90),
            volumes: Some(volumes),
            ..Default::default()
        }),
    };
    if let Some(pod_template_overrides) = minecraft_server.spec.pod_overrides.clone() {
        pod_template.merge_from(pod_template_overrides);
    }
    pod_template
}

fn server_provisioner_init_container(
    minecraft_server: &MinecraftServerV1Alpha1,
    archives: &mut Vec<String>,
) -> Container {
    let repos = minecraft_server
        .spec
        .template
        .as_ref()
        .and_then(|template| match template {
            MinecraftTemplateSpec::Ref(_) => None,
            MinecraftTemplateSpec::Git(git) => Some(git),
        });
    provisioner_init_container(
        minecraft_server.spec.provisioner.as_ref(),
        repos.map(|a| a.as_slice()),
        archives,
    )
}

fn minecraft_container(
    name: &str,
    namespace: &str,
    minecraft_server: &MinecraftServerV1Alpha1,
    archives: &[String],
) -> Container {
    let container_image = minecraft_server
        .spec
        .image
        .as_deref()
        .unwrap_or(IMAGE_MINECRAFT_SERVER)
        .to_string();

    let mut resource = ResourceRequirements::default();
    let mut env = Vec::with_capacity(16);
    minecraft_server_env(name, namespace, minecraft_server, &mut env);
    minecraft_server_env_templates("/template", archives, &mut env);
    minecraft_resources(
        minecraft_server.spec.resources.as_ref(),
        &mut env,
        &mut resource,
    );

    Container {
        name: "minecraft".to_string(),
        image: Some(container_image),
        ports: Some(vec![ContainerPort {
            name: Some("minecraft".to_string()),
            container_port: 25565,
            protocol: Some("TCP".to_string()),
            ..Default::default()
        }]),
        env: Some(env),
        stdin: Some(true),
        tty: Some(true),
        stdin_once: Some(false),
        startup_probe: Some(Probe {
            exec: Some(ExecAction {
                command: Some(vec!["mc-health".to_string()]),
            }),
            initial_delay_seconds: Some(5),
            period_seconds: Some(5),
            failure_threshold: Some(60),
            ..Default::default()
        }),
        liveness_probe: Some(Probe {
            exec: Some(ExecAction {
                command: Some(vec!["mc-health".to_string()]),
            }),
            initial_delay_seconds: Some(5),
            period_seconds: Some(5),
            failure_threshold: Some(10),
            ..Default::default()
        }),
        volume_mounts: Some(vec![
            VolumeMount {
                name: VOLUME_NAME_MINECRAFT_SERVER.to_string(),
                mount_path: "/data".to_string(),
                ..Default::default()
            },
            VolumeMount {
                name: VOLUME_NAME_TEMPLATE.to_string(),
                mount_path: "/template".to_string(),
                ..Default::default()
            },
        ]),
        resources: Some(resource),
        ..Default::default()
    }
}

fn minecraft_server_env_templates(
    templates_dir: &str,
    archives: &[String],
    into: &mut Vec<EnvVar>,
) {
    if !archives.is_empty() {
        let archives = archives
            .iter()
            .map(|a| format!("{}/{}", templates_dir, a))
            .collect::<Vec<_>>();
        into.push(EnvVar {
            name: "GENERIC_PACKS".to_string(),
            value: Some(archives.join(",")),
            value_from: None,
        });
    }
}

fn minecraft_server_env(
    name: &str,
    namespace: &str,
    minecraft_server: &MinecraftServerV1Alpha1,
    into: &mut Vec<EnvVar>,
) {
    into.push(EnvVar {
        name: "EULA".to_string(),
        value: Some("true".to_string()),
        value_from: None,
    });
    into.push(EnvVar {
        name: "DEBUG_EXEC".to_string(),
        value: Some("true".to_string()),
        value_from: None,
    });
    into.push(EnvVar {
        name: "PROXY".to_string(),
        value: Some(format!("{}:{}", SQUID_PROXY_NAME, SQUID_PROXY_PORT)),
        value_from: None,
    });
    into.push(EnvVar {
        name: "SERVER_NAME".to_string(),
        value: None,
        value_from: Some(EnvVarSource {
            field_ref: Some(ObjectFieldSelector {
                field_path: "metadata.name".to_string(),
                api_version: None,
            }),
            ..Default::default()
        }),
    });
    into.push(EnvVar {
        name: "GUI".to_string(),
        value: Some("FALSE".to_string()),
        value_from: None,
    });
    into.push(EnvVar {
        name: "EXEC_DIRECTLY".to_string(),
        value: Some("TRUE".to_string()),
        value_from: None,
    });
    into.push(EnvVar {
        name: "SERVER_PORT".to_string(),
        value: Some("25565".to_string()),
        value_from: None,
    });
    into.push(EnvVar {
        name: "ONLINE_MODE".to_string(),
        value: Some("FALSE".to_string()),
        value_from: None,
    });
    into.push(EnvVar {
        name: "USE_AIKAR_FLAGS".to_string(),
        value: Some("TRUE".to_string()),
        value_from: None,
    });
    into.push(EnvVar {
        name: "USE_SIMD_FLAGS".to_string(),
        value: Some("TRUE".to_string()),
        value_from: None,
    });
    into.push(EnvVar {
        name: "USE_FLARE_FLAGS".to_string(),
        value: Some("TRUE".to_string()),
        value_from: None,
    });

    //TODO enable rcon in certain cases (f.e. backup sidecar)
    into.push(EnvVar {
        name: "ENABLE_RCON".to_string(),
        value: Some("FALSE".to_string()),
        value_from: None,
    });

    if let Some(install) = &minecraft_server.spec.install {
        if let Some(spigot_plugins) = &install.spigot {
            let spigot_plugins = spigot_plugins
                .into_iter()
                .filter(|i| i > &&0_i32)
                .map(|i| i.to_string())
                .collect::<Vec<_>>();

            into.push(EnvVar {
                name: "SPIGET_RESOURCES".to_string(),
                value: Some(spigot_plugins.join(",")),
                value_from: None,
            });
        }
        if let Some(modrinth_resources) = &install.modrinth {
            into.push(EnvVar {
                name: "MODRINTH_RESOURCES".to_string(),
                value: Some(modrinth_resources.join("\n")),
                value_from: None,
            });
        }

        if let Some(install_server) = &install.software {
            let (server_type, version) = match install_server {
                MinecraftServerInstallSpecServerSoftware::Paper { paper } => {
                    ("paper".to_string(), paper.clone())
                }
                MinecraftServerInstallSpecServerSoftware::Purpur { purpur } => {
                    ("purpur".to_string(), purpur.clone())
                }
                MinecraftServerInstallSpecServerSoftware::Folia { folia } => {
                    ("folia".to_string(), folia.clone())
                }
            };
            into.push(EnvVar {
                name: "TYPE".to_string(),
                value: Some(server_type),
                value_from: None,
            });
            if !version.is_empty() {
                into.push(EnvVar {
                    name: "VERSION".to_string(),
                    value: Some(version),
                    value_from: None,
                });
            }
        }
    }
}
