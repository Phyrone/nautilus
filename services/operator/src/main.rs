use std::pin::pin;
use clap::Parser;
use error_stack::{Report, ResultExt};
use futures::{StreamExt, TryStreamExt};
use k8s_openapi::api::core::v1::{Container, Pod, PodSpec};
use k8s_openapi::apimachinery::pkg::apis::meta::v1::ObjectMeta;
use kube::api::{DeleteParams, Patch, PatchParams, PostParams, WatchEvent, WatchParams};
use kube::{Api, Client};
use nautilus_shared_crds::{all_crds_exist, apply_crds, delete_crds};
use nautilus_shared_lib::logging::init_logger;
use std::time::Duration;
use thiserror::Error;
use tokio::time::{sleep, Instant};
use tracing::{info, instrument};
use nautilus_shared_crds::service::MinecraftService;
use crate::startup::ResourceDefinitionsStrategy;

mod startup;
mod service;

#[derive(Debug, Error)]
pub enum AppError {
    #[error("an error occurred initializing the tokio runtime")]
    InitRuntime,

    #[error("cannot create k8s client")]
    K8sClient,

    #[error("something went wrong while applying or checking crds")]
    ApplyCrds,
}

fn main() -> error_stack::Result<(), AppError> {
    let params = startup::StartupParams::parse();
    init_logger(&params.logging_params);
    let runtime = nautilus_shared_lib::tokio::init_tokio_runtime(&params.tokio_runtime_params)
        .change_context(AppError::InitRuntime)?;

    runtime.block_on(async_main(params, &runtime))?;
    Ok(())
}

#[instrument(level = "debug")]
async fn async_main(
    params: startup::StartupParams,
    runtime: &tokio::runtime::Runtime,
) -> error_stack::Result<(), AppError> {
    info!("Connecting to kubernetes...");
    let time = Instant::now();
    let client = Client::try_default()
        .await
        .change_context(AppError::K8sClient)?;
    let time = time.elapsed();
    info!("Connection to kubernetes established ({:?})", time);
    startup_apply_crds(client.clone(), &params)
        .await
        .change_context(AppError::ApplyCrds)?;

    let services = Api::<MinecraftService>::all(client.clone());
    
    
    info!("Watching services");
    let mut services = services.watch(&WatchParams::default(), "0")
        .await
        .change_context(AppError::K8sClient)?
        .boxed();

    while let Some(event) = services.try_next().await.unwrap() {
        match event {
            WatchEvent::Added(a) => {
                info!("Added: {:?}", a.metadata.name);
            }
            WatchEvent::Modified(a) => {
                info!("Modified: {:?}", a.metadata.name);
            }
            WatchEvent::Deleted(a) => {
                info!("Deleted: {:?}", a.metadata.name);
            }
            WatchEvent::Bookmark(a) => {
                info!("Bookmark: {:?}", serde_json::to_string(&a).unwrap());
            }
            WatchEvent::Error(a) => {}
        }
    }

    Ok(())
}

#[derive(Debug, Error)]
enum ApplyCrdsError {
    #[error("error when applying crds")]
    Apply,
    #[error("error when checking crds")]
    Check,
    #[error("some crds are missing")]
    Missing,
}

async fn startup_apply_crds(
    client: Client,
    params: &startup::StartupParams,
) -> error_stack::Result<(), ApplyCrdsError> {
    match &params.crds {
        ResourceDefinitionsStrategy::Fail => {
            let all_crds_exist = all_crds_exist(client).await
                .change_context(ApplyCrdsError::Check)?;
            if !all_crds_exist {
                return Err(Report::new(ApplyCrdsError::Missing));
            }
        }
        ResourceDefinitionsStrategy::Apply => {
            apply_crds(client).await
                .change_context(ApplyCrdsError::Apply)?;
        }
        ResourceDefinitionsStrategy::Ignore => {}
    }

    Ok(())
}
