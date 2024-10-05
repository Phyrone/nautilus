use std::time::Duration;
use clap::Parser;
use error_stack::ResultExt;
use futures::{StreamExt, TryStreamExt};
use k8s_openapi::api::core::v1::{Container, Pod, PodSpec};
use k8s_openapi::apimachinery::pkg::apis::meta::v1::ObjectMeta;
use kube::api::{DeleteParams, Patch, PatchParams, PostParams, WatchEvent, WatchParams};
use kube::{Api, Client};
use nautilus_shared_crds::{apply_crds, delete_crds};
use nautilus_shared_lib::logging::init_logger;
use thiserror::Error;
use tokio::time::{sleep, Instant};
use tracing::info;
use crate::startup::{CrdSubbcommands, Subcommands};

mod startup;

#[derive(Debug, Error)]
pub enum AppError {
    #[error("an error occurred initializing the tokio runtime")]
    InitRuntime,

    #[error("cannot create k8s client")]
    K8sClient,
}

fn main() -> error_stack::Result<(), AppError> {
    let params = startup::StartupParams::parse();
    init_logger(&params.logging_params);
    let runtime = nautilus_shared_lib::tokio::init_tokio_runtime(&params.tokio_runtime_params)
        .change_context(AppError::InitRuntime)?;


    runtime.block_on(async_main(params, &runtime))?;
    Ok(())
}

async fn async_main(
    params: startup::StartupParams,
    runtime: &tokio::runtime::Runtime,
) -> error_stack::Result<(), AppError> {
    info!("Connecting to kubernetes...");
    let time = Instant::now();
    let client = Client::try_default().await
        .change_context(AppError::K8sClient)?;
    let time = time.elapsed();
    info!("Connection to kubernetes established ({:?})", time);
    info!(" default-namespace: {}", client.default_namespace());
    //let a = Api::<ServerFleet>::all(client.clone());

    match &params.subcommand {
        None => {
            
            let mut pods = Vec::new();
            for i in 0..30{
                let api = Api::<Pod>::namespaced(client.clone(), "mcnet");
                let pod = Pod {
                    status: None,
                    metadata: ObjectMeta {
                        generate_name: Some("bedwars-".to_string()),
                        namespace: Some("mcnet".to_string()),
                        ..Default::default()
                    },
                    spec: Some(PodSpec {
                        containers: vec![Container {
                            name: "nginx".to_string(),
                            image: Some("nginx".to_string()),
                            ..Default::default()
                        }],
                        ..Default::default()
                    }),
                };
                let created = api.create(&PostParams::default(), &pod).await.unwrap();
                pods.push(created);
            }
            println!("Created pods");
            sleep(Duration::from_secs(30)).await;
            for pod in pods {
                let api = Api::<Pod>::namespaced(client.clone(), "mcnet");
                let _ = api.delete(&pod.metadata.name.unwrap(), &DeleteParams::default()).await;
            }
            println!("Deleted pods");
        }
        Some(Subcommands::Crd(crd_params)) => {
            handle_crds_subcommand(client.clone(), crd_params).await;
        }
    }


    Ok(())
}

async fn handle_crds_subcommand(
    client: Client,
    params: &startup::CRDSubcommandParams,
) {
    match &params.subcommand {
        CrdSubbcommands::Apply => {
            info!("Applying CRDs...");
            let time = Instant::now();
            apply_crds(client).await.unwrap();
            let time = time.elapsed();
            info!("CRDs applied successfully ({:?})", time);
        }
        CrdSubbcommands::Delete => {
            info!("Deleting CRDs...");
            let time = Instant::now();
            delete_crds(client).await;
            let time = time.elapsed();
            info!("CRDs deleted successfully ({:?})", time);
        }
        CrdSubbcommands::Export => {
            todo!()
        }
    }
}
