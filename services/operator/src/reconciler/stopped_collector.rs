use crate::consts::CONTAINER_NAME_SERVER;
use crate::reconciler::{
    Context, RunReconcilerError, DEFAULT_RECONCILE_INTERVAL, ERROR_RECONCILE_INTERVAL,
};
use futures::StreamExt;
use k8s_openapi::api::core::v1::Pod;
use kube::api::DeleteParams;
use kube::runtime::controller::Action;
use kube::runtime::watcher::Config as WatcherConfig;
use kube::runtime::Config as ControllerConfig;
use kube::runtime::{watcher, Controller};
use kube::{Api, ResourceExt};
use log::debug;
use std::sync::Arc;
use std::time::Duration;
use thiserror::Error;
use tokio::pin;
use tokio_util::sync::CancellationToken;
use tracing::{info, warn};

pub async fn run(
    ctx: Arc<Context>,
    shutdown: CancellationToken,
) -> error_stack::Result<(), RunReconcilerError> {
    let pods_endpoint = Api::<Pod>::all(ctx.client.clone());
    let config = WatcherConfig {
        label_selector: Some("nautilus.phyrone.de/on-stop=delete".to_string()),
        ..Default::default()
    };
    let reconcile_stream = Controller::new(pods_endpoint, config.clone())
        .graceful_shutdown_on(shutdown.cancelled_owned())
        .run(reconcile, on_error, ctx);
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

fn on_error(pod: Arc<Pod>, error: &ReconcileError, ctx: Arc<Context>) -> Action {
    warn!(
        "Stop collector failed for pod {}: {}",
        pod.name_unchecked(),
        error
    );
    Action::requeue(ERROR_RECONCILE_INTERVAL)
}

async fn reconcile(pod: Arc<Pod>, ctx: Arc<Context>) -> Result<Action, ReconcileError> {
    let name = pod.name_unchecked();
    let namespace = pod.metadata.namespace.as_ref();

    let namespace = if let Some(namespace) = namespace {
        namespace.as_str()
    } else {
        debug!("Pod {} has no namespace, retrying...", name);
        return Ok(Action::requeue(Duration::from_secs(10)));
    };
    let pods_endpoint = Api::<Pod>::namespaced(ctx.client.clone(), namespace);

    let container_statuses = pod
        .status
        .as_ref()
        .and_then(|status| status.container_statuses.as_ref());

    if let Some(container_statuses) = container_statuses {
        for container_status in container_statuses {
            if container_status.restart_count > 0 && container_status.name == CONTAINER_NAME_SERVER
            {
                debug!(
                    "Pod {} is marked as delete on stop and restart {} times, deleting...",
                    name, container_status.restart_count
                );
                let time = tokio::time::Instant::now();
                pods_endpoint
                    .delete(
                        &name,
                        &DeleteParams {
                            grace_period_seconds: Some(0),
                            ..Default::default()
                        },
                    )
                    .await
                    .map_err(ReconcileError::KubeClient)?;
                let time = time.elapsed();
                info!("Deleted pod {} ({:?})", name, time);
            }
        }
    }

    Ok(Action::requeue(DEFAULT_RECONCILE_INTERVAL))
}
