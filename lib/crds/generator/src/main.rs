use crate::startup::{MainInstruction, StartupParams};
use clap::Parser;
use indicatif::{MultiProgress, ProgressBar, ProgressStyle};
use k8s_openapi::apiextensions_apiserver::pkg::apis::apiextensions::v1::CustomResourceDefinition;
use kube::api::{DeleteParams, Patch, PatchParams};
use kube::{Api, ResourceExt};
use std::time::Duration;
use tokio::task::JoinSet;

mod crds;
mod startup;

#[tokio::main]
async fn main() {
    let params = StartupParams::parse();

    let crds = crds::all();

    match params.subcommand {
        MainInstruction::Export(instruction) => {
            if instruction.single_file {
                let mut buffer = String::new();
                for crd in crds {
                    buffer.push_str("---\n");
                    buffer.push_str(&serde_yml::to_string(&crd).expect("Failed to serialize CRD"));
                }
                if let Some(output) = instruction.output {
                    //check if the output is a directory
                    if output.is_dir() {
                        let file_name = "crds.yaml";
                        let output_file = output.join(file_name);
                        tokio::fs::write(output_file, buffer)
                            .await
                            .expect("Failed to write file");
                    } else {
                        tokio::fs::write(output, buffer)
                            .await
                            .expect("Failed to write file");
                    }
                }
            } else {
                let target_dir = instruction.output.unwrap_or(".".into());
                for crd in crds {
                    let name = crd.name_any();
                    let file_name = format!("{}.yaml", name);
                    let file_yaml = serde_yml::to_string(&crd).expect("Failed to serialize CRD");
                    let output_file = target_dir.join(file_name);
                    tokio::fs::write(output_file, file_yaml)
                        .await
                        .expect("Failed to write file");
                }
            }
        }
        MainInstruction::Print => {
            for crd in crds {
                println!("---");
                println!(
                    "{}",
                    serde_yml::to_string(&crd).expect("Failed to serialize CRD")
                );
            }
        }
        MainInstruction::Apply => {
            let client = kube::client::Client::try_default()
                .await
                .expect("Failed to create client");
            let multi_progress = MultiProgress::new();
            let mut jobs = JoinSet::new();
            let crds_api = Api::<CustomResourceDefinition>::all(client.clone());
            let params = PatchParams::apply("nautilus-crd-generator");
            for crd in crds {
                let progress = ProgressBar::new(1);
                progress.set_style(ProgressStyle::default_spinner());
                progress.enable_steady_tick(Duration::from_millis(100));
                let progress = multi_progress.add(progress);
                let name = crd.name_any();
                let patch = Patch::Apply(crd);
                let params = params.clone();
                let crds_api = crds_api.clone();
                jobs.spawn(async move {
                    let result = crds_api.patch(&name, &params, &patch).await;
                    match result {
                        Ok(_) => progress.finish_with_message("Done"),
                        Err(err) => progress.finish_with_message(format!("Failed: {}", err)),
                    }
                });
            }
            jobs.join_all().await;
        }
        MainInstruction::Delete => {
            let client = kube::client::Client::try_default()
                .await
                .expect("Failed to create client");
            let crds_api = Api::<CustomResourceDefinition>::all(client.clone());
            let mut jobs = JoinSet::new();
            let progresses = MultiProgress::new();

            for crd in crds {
                let crds_api = crds_api.clone();
                let progress = ProgressBar::new(1);
                progress.enable_steady_tick(Duration::from_millis(100));
                progress.set_style(ProgressStyle::default_spinner());
                let progress = progresses.add(progress);
                jobs.spawn(async move {
                    let name = crd.name_any();
                    let result = crds_api.delete(&name, &DeleteParams::default()).await;
                    match result {
                        Ok(_) => progress.finish_with_message("Done"),
                        Err(err) => progress.finish_with_message(format!("Failed: {}", err)),
                    }
                });
            }
            jobs.join_all().await;
        }
    }
}
