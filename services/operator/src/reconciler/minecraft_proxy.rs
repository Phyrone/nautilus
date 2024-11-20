use crate::reconciler::{
    Context, RunReconcilerError, DEFAULT_RECONCILE_INTERVAL, ERROR_RECONCILE_INTERVAL,
};
use futures::StreamExt;
use k8s_openapi::api::apps::v1::Deployment;
use kube::runtime::controller::Action;
use kube::runtime::reflector::ObjectRef;
use kube::runtime::Controller;
use kube::Api;
use lib_crds::crds::minecraft_cluster::MinecraftClusterV1Alpha1;
use lib_crds::crds::minecraft_proxy::MinecraftProxyV1Alpha1;
use std::sync::Arc;
use thiserror::Error;
use tokio::pin;
use tokio_util::sync::CancellationToken;
use tracing::warn;

pub async fn run(
    ctx: Arc<Context>,
    shutdown: CancellationToken,
) -> error_stack::Result<(), RunReconcilerError> {
    let proxies_endpoint = Api::<MinecraftProxyV1Alpha1>::all(ctx.client.clone());
    let clusters_endpoint = Api::<MinecraftClusterV1Alpha1>::all(ctx.client.clone());
    let deployments = Api::<Deployment>::all(ctx.client.clone());
    let config = kube::runtime::watcher::Config::default();

    let reconcile_stream = Controller::new(proxies_endpoint, config.clone())
        .graceful_shutdown_on(shutdown.cancelled_owned())
        .owns(deployments, config.clone())
        .watches(clusters_endpoint, config.clone(), cluster_proxy_map)
        .run(reconcile, reconcile_error, ctx);

    pin!(reconcile_stream);
    while let Some(event) = reconcile_stream.next().await {
        //TODO handle event
    }

    Ok(())
}

#[derive(Error, Debug)]
pub enum ReconcileError {
    #[error("kube client failed: {0}")]
    KubeClient(#[from] kube::Error),
}

fn cluster_proxy_map(cluster: MinecraftClusterV1Alpha1) -> Vec<ObjectRef<MinecraftProxyV1Alpha1>> {
    cluster
        .status
        .map(|status| status.proxies)
        .map(|proxies| {
            proxies
                .into_iter()
                .filter_map(|proxy| {
                    let name = proxy.name?;
                    let namespace = proxy.namespace?;
                    Some(ObjectRef::new(&name).within(&namespace))
                })
                .collect()
        })
        .unwrap_or_default()
}

fn reconcile_error(
    proxy: Arc<MinecraftProxyV1Alpha1>,
    error: &ReconcileError,
    ctx: Arc<Context>,
) -> Action {
    warn!("Reconcile error: {}", error);
    Action::requeue(ERROR_RECONCILE_INTERVAL)
}

async fn reconcile(
    proxy: Arc<MinecraftProxyV1Alpha1>,
    ctx: Arc<Context>,
) -> Result<Action, ReconcileError> {
    Ok(Action::requeue(DEFAULT_RECONCILE_INTERVAL))
}
