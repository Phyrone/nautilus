use clap::{Args, Parser, Subcommand};
use k8s_openapi::chrono::format::Pad;
use steve_shared_lib::logging::LoggingParams;
use steve_shared_lib::tokio::TokioRuntimeParams;

#[derive(Debug, Clone, Parser)]
#[clap(version, author)]
pub struct StartupParams {
    #[clap(flatten)]
    pub tokio_runtime_params: TokioRuntimeParams,

    #[clap(flatten)]
    pub logging_params: LoggingParams,

    #[clap(subcommand)]
    pub subcommand: Option<Subcommands>,
}

#[derive(Debug, Clone, Subcommand)]
pub enum Subcommands {
    Crd(CRDSubcommandParams),
}
#[derive(Debug, Clone, Args)]
pub struct CRDSubcommandParams {
    #[clap(subcommand)]
    pub subcommand: CrdSubbcommands,
}
#[derive(Debug, Clone, Subcommand)]
pub enum CrdSubbcommands {
    Apply,
    Delete,
    Export,
}
