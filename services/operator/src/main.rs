use crate::consts::FIELD_MANAGER_NAME;
use crate::startup::{CrdsStrategy, StartupParams};
use clap::Parser;
use error_stack::{FutureExt, Report, ResultExt};
use futures::StreamExt;
use k8s_openapi::apiextensions_apiserver::pkg::apis::apiextensions::v1::CustomResourceDefinition;
use kube::api::{Patch, PatchParams};
use kube::{Api, Client, ResourceExt};
use std::process::exit;
use std::time::Duration;
use thiserror::Error;
use tokio::task::JoinSet;
use tokio::time::{timeout, Instant};
use tokio_util::sync::CancellationToken;
use tracing::info;

mod consts;
mod env;
mod reconciler;
mod startup;
mod utils;

#[derive(Error, Debug)]
#[error("An critical error occurred while running the operator")]
pub struct RunOperatorError;

#[tokio::main]
async fn main() -> error_stack::Result<(), RunOperatorError> {
    let params = StartupParams::parse();

    pretty_env_logger::env_logger::builder()
        .format_timestamp_secs()
        .default_format()
        .filter_level(params.verbosity.log_level_filter())
        .init();

    let client = kube::Client::try_default()
        .await
        .change_context(RunOperatorError)?;
    run_crds(&client, &params.crds)
        .await
        .change_context(RunOperatorError)?;

    let shutdown = CancellationToken::new();
    let shutdown_trigger = shutdown.clone();
    let guard = shutdown_trigger.drop_guard();
    let task =
        tokio::spawn(reconciler::run(client, shutdown.clone()).change_context(RunOperatorError));

    tokio::signal::ctrl_c()
        .await
        .change_context(RunOperatorError)?;
    shutdown.cancel();
    info!("Interrupt received, shutting down...");
    let shutdown_result = timeout(Duration::from_secs(10), task).await;
    drop(guard);
    match shutdown_result {
        Ok(Ok(Ok(s))) => exit(0),
        Ok(Ok(Err(op_error))) => Err(op_error).change_context(RunOperatorError)?,
        Ok(Err(join_error)) => Err(join_error).change_context(RunOperatorError)?,
        Err(timeout) => {
            let error_msg = format!("shutdown grace period expired after {:?}", timeout);
            Err(timeout)
                .change_context(RunOperatorError)
                .attach_printable(error_msg)?;
        }
    }

    Ok(())
}

#[derive(Error, Debug)]
pub enum CrdsError {
    #[error("{count} resources whre not applied successfully")]
    Apply { count: usize },
    #[error("{count} resources are not valid")]
    Invalid { count: usize },
}

async fn run_crds(client: &Client, strategy: &CrdsStrategy) -> error_stack::Result<(), CrdsError> {
    let crds_endpoint = Api::<CustomResourceDefinition>::all(client.clone());
    let crds = lib_crds::all();
    match strategy {
        CrdsStrategy::None => {}
        CrdsStrategy::Validate => {
            todo!("validate crds")
        }
        CrdsStrategy::Apply => {
            info!("Updating CRDs if necessary...");
            let time = Instant::now();
            let mut tasks = JoinSet::new();
            let params = PatchParams::apply(FIELD_MANAGER_NAME).force();

            for crd in crds {
                let crds_endpoint = crds_endpoint.clone();
                let params = params.clone();
                tasks.spawn(async move {
                    let name = crd.name_unchecked();
                    let patch = Patch::Apply(crd);
                    let result = crds_endpoint.patch(&name, &params, &patch).await;
                    (name, result)
                });
            }
            let all = tasks.join_all().await;
            let mut errors = Vec::new();
            for (name, result) in all {
                if let Err(error) = result {
                    errors.push((name, error));
                }
            }
            if !errors.is_empty() {
                let error = CrdsError::Apply {
                    count: errors.len(),
                };
                let mut report = Report::new(error);
                for (name, error) in errors {
                    report = report.attach_printable(format!("{}: {}", name, error));
                }
                return Err(report);
            }
            info!("CRDs updated ({:?})", time.elapsed());
        }
    }
    Ok(())
}
