use error_stack::ResultExt;
use kube::Client;
use std::sync::Arc;
use std::time::Duration;
use thiserror::Error;
use tokio::try_join;
use tokio_util::sync::CancellationToken;

mod minecraft_cluster;
mod minecraft_proxy;
mod minecraft_server;
mod shared;
mod stopped_collector;

const DEFAULT_RECONCILE_INTERVAL: Duration = Duration::from_secs(3600);
const ERROR_RECONCILE_INTERVAL: Duration = Duration::from_secs(30);

#[derive(Debug, Error)]
#[error("An error occurred while tracking {0}")]
pub struct RunReconcilerError(&'static str);

#[derive(Debug, Error)]
#[error("An error occurred while running the reconcilers")]
pub struct RunReconcilersError;

#[derive(Clone)]
pub struct Context {
    client: kube::Client,
}
impl Context {
    fn new(client: kube::Client) -> Self {
        Self { client }
    }
}

pub async fn run(
    client: Client,
    shutdown: CancellationToken,
) -> error_stack::Result<(), RunReconcilersError> {
    let ctx = Arc::new(Context::new(client));

    try_join!(
        minecraft_server::run(ctx.clone(), shutdown.clone()),
        minecraft_cluster::run(ctx.clone(), shutdown.clone()),
        minecraft_proxy::run(ctx.clone(), shutdown.clone()),
        stopped_collector::run(ctx.clone(), shutdown.clone())
    )
    .change_context(RunReconcilersError)?;

    Ok(())
}
