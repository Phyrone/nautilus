use std::collections::BTreeMap;
use crate::consts::{FIELD_MANAGER_NAME, IMAGE_SQUID_PROXY, LABEL_K8S_APP_COMPONENT, LABEL_K8S_APP_NAME, LABEL_NAUTILUS_GROUP, LABEL_NAUTILUS_TYPE, SQUID_PROXY_NAME, SQUID_PROXY_PORT};
use crate::reconciler::{
    Context, RunReconcilerError, DEFAULT_RECONCILE_INTERVAL, ERROR_RECONCILE_INTERVAL,
};
use crate::utils::object_ref_matches;
use futures::{StreamExt, TryStreamExt};
use k8s_openapi::api::core::v1::{Container, ContainerPort, ObjectReference, PodSpec, PodTemplateSpec, Service, ServicePort, ServiceSpec};
use kube::api::{ListParams, Object, Patch, PatchParams, PostParams};
use kube::runtime::controller::Action;
use kube::runtime::reflector::Lookup;
use kube::runtime::watcher::Config;
use kube::runtime::Controller;
use kube::{Api, Client, Resource, ResourceExt};
use lib_crds::crds::minecraft_cluster::{MinecraftClusterV1Alpha1, MinecraftClusterV1Alpha1Status};
use lib_crds::crds::minecraft_proxy::MinecraftProxyV1Alpha1;
use lib_crds::crds::minecraft_server::MinecraftServerV1Alpha1;
use std::sync::Arc;
use k8s_openapi::api::apps::v1::{Deployment, DeploymentSpec};
use k8s_openapi::apimachinery::pkg::apis::meta::v1::{LabelSelector, ObjectMeta, OwnerReference};
use thiserror::Error;
use tokio::pin;
use tokio::time::Instant;
use tokio_util::sync::CancellationToken;
use tracing::{info, warn};

pub async fn run(
    ctx: Arc<Context>,
    shutdown: CancellationToken,
) -> error_stack::Result<(), RunReconcilerError> {
    let clusters_endpoint = Api::<MinecraftClusterV1Alpha1>::all(ctx.client.clone());
    let minecraft_servers_endpoint = Api::<MinecraftServerV1Alpha1>::all(ctx.client.clone());
    let minecraft_proxys_endpoint = Api::<MinecraftProxyV1Alpha1>::all(ctx.client.clone());

    let config = Config::default();

    let reconcile_stream = Controller::new(clusters_endpoint, config.clone())
        .graceful_shutdown_on(shutdown.cancelled_owned())
        .owns(minecraft_servers_endpoint, config.clone())
        .owns(minecraft_proxys_endpoint, config.clone())
        .run(reconcile, on_reconcile_error, ctx.clone());

    pin!(reconcile_stream);
    while let Some(result) = reconcile_stream.next().await {
        //TODO: Logging and error handling
    }

    Ok(())
}

#[derive(Error, Debug)]
pub enum ReconcileError {
    #[error("kube client failed: {0}")]
    KubeClient(#[from] kube::Error),
}

fn on_reconcile_error(
    server: Arc<MinecraftClusterV1Alpha1>,
    error: &ReconcileError,
    ctx: Arc<Context>,
) -> Action {
    warn!(
        "Failed to reconcile MinecraftServer {}: {}",
        server.name_unchecked(),
        error
    );
    Action::requeue(ERROR_RECONCILE_INTERVAL)
}

async fn reconcile(
    mut cluster: Arc<MinecraftClusterV1Alpha1>,
    ctx: Arc<Context>,
) -> Result<Action, ReconcileError> {
    let name = cluster.name_unchecked();
    let namespace = cluster
        .metadata
        .namespace
        .as_ref()
        .expect("MinecraftCluster must have a namespace when taken from the API")
        .as_str();
    let time = Instant::now();
    let clusters_endpoint =
        Api::<MinecraftClusterV1Alpha1>::namespaced(ctx.client.clone(), namespace);
    let servers_endpoint =
        Api::<MinecraftServerV1Alpha1>::namespaced(ctx.client.clone(), namespace);

    let servers = servers_endpoint
        .list(&ListParams::default())
        .await
        .map_err(ReconcileError::KubeClient)?
        .into_iter()
        .filter(|server| is_in_cluster(server, &cluster))
        .collect::<Vec<_>>();
    get_download_proxy(&ctx.client, &cluster, namespace).await
        .map_err(ReconcileError::KubeClient)?;
    
    let cluster_update = Arc::make_mut(&mut cluster);
    let status = cluster_update
        .status
        .get_or_insert(MinecraftClusterV1Alpha1Status::default());
    
    
    status.servers.clear();
    status.servers.reserve(servers.len());
    for server in &servers {
        status.servers.push(ObjectReference {
            name: server.metadata.name.clone(),
            uid: server.metadata.uid.clone(),
            namespace: server.metadata.namespace.clone(),
            ..Default::default()
        });
    }
    //clusters_endpoint.patch_status(&name, &PatchParams::apply(FIELD_MANAGER_NAME), &Patch::Apply(&cluster_update)).await.map_err(ReconcileError::KubeClient)?;
    clusters_endpoint
        .replace_status(
            &name,
            &PostParams {
                field_manager: Some(FIELD_MANAGER_NAME.to_string()),
                ..Default::default()
            },
            serde_json::to_vec(&cluster_update).expect("Failed to serialize MinecraftCluster"),
        )
        .await
        .map_err(ReconcileError::KubeClient)?;
    
    let owner_reference = cluster
        .controller_owner_ref(&())
        .expect("owner reference should have been created");
    for mut server in servers {
        server.metadata.owner_references = Some(vec![owner_reference.clone()]);
        
        servers_endpoint.replace(&server.name_unchecked(),&PostParams::default(),&server)
            .await
            .map_err(ReconcileError::KubeClient)?;
    }
    

    let time = time.elapsed();
    info!("Reconciled cluster {} ({:?})", name, time);
    Ok(Action::requeue(DEFAULT_RECONCILE_INTERVAL))
}

fn is_in_cluster(server: &MinecraftServerV1Alpha1, cluster: &MinecraftClusterV1Alpha1) -> bool {
    object_ref_matches(&server.spec.cluster, &cluster.metadata, None, None)
}

async fn get_download_proxy(
    client: &Client,
    cluster: &MinecraftClusterV1Alpha1,
    namespace: &str,
) -> kube::Result<()> {
    let deployments = Api::<Deployment>::namespaced(client.clone(), namespace);
    let services = Api::<Service>::namespaced(client.clone(), namespace);
    let mut selector_labels = BTreeMap::new();
    selector_labels.insert(LABEL_K8S_APP_NAME.to_string(), "squid-proxy".to_string());
    selector_labels.insert(LABEL_NAUTILUS_TYPE.to_string(), "cache".to_string());
    selector_labels.insert(LABEL_K8S_APP_COMPONENT.to_string(), "cache".to_string());
    selector_labels.insert(LABEL_NAUTILUS_GROUP.to_string(), "squid-proxy".to_string());

    let pod_labels = selector_labels.clone();
    let owner_reference = cluster
        .controller_owner_ref(&())
        .expect("owner reference should have been created");

    let squid_deployment = Deployment {
        metadata: ObjectMeta {
            name: Some(SQUID_PROXY_NAME.to_string()),
            namespace: Some(namespace.to_string()),
            owner_references: Some(vec![owner_reference]),
            ..Default::default()
        },
        spec: Some(DeploymentSpec {
            replicas: Some(1),
            selector: LabelSelector {
                match_labels: Some(selector_labels.clone()),
                ..Default::default()
            },
            template: PodTemplateSpec {
                metadata: Some(ObjectMeta {
                    labels: Some(pod_labels),
                    deletion_grace_period_seconds: Some(30),

                    ..Default::default()
                }),
                spec: Some(PodSpec {
                    containers: vec![Container {
                        name: "squid-proxy".to_string(),
                        image: Some(IMAGE_SQUID_PROXY.to_string()),
                        ports: Some(vec![ContainerPort {
                            container_port: SQUID_PROXY_PORT,
                            name: Some("proxy".to_string()),
                            protocol: Some("TCP".to_string()),
                            ..Default::default()
                        }]),
                        ..Default::default()
                    }],
                    ..Default::default()
                }),
            },
            ..Default::default()
        }),
        ..Default::default()
    };
    let squid_service = Service {
        metadata: ObjectMeta {
            name: Some("squid-proxy".to_string()),
            namespace: Some(namespace.to_string()),
            ..Default::default()
        },
        spec: Some(ServiceSpec {
            selector: Some(selector_labels),
            type_: Some("ClusterIP".to_string()),
            ip_family_policy: Some("PreferDualStack".to_string()),
            ports: Some(vec![ServicePort {
                port: SQUID_PROXY_PORT,
                name: Some("proxy".to_string()),
                protocol: Some("TCP".to_string()),
                ..Default::default()
            }]),
            ..Default::default()
        }),
        ..Default::default()
    };
    services
        .patch(
            SQUID_PROXY_NAME,
            &PatchParams::apply(FIELD_MANAGER_NAME).force(),
            &Patch::Apply(squid_service),
        )
        .await?;
    deployments
        .patch(
            SQUID_PROXY_NAME,
            &PatchParams::apply(FIELD_MANAGER_NAME).force(),
            &Patch::Apply(squid_deployment),
        )
        .await?;
    Ok(())
}
