use clap::{Args, Parser, Subcommand, ValueEnum};
use k8s_openapi::chrono::format::Pad;
use nautilus_shared_lib::logging::LoggingParams;
use nautilus_shared_lib::tokio::TokioRuntimeParams;

#[derive(Debug, Clone, Parser)]
#[clap(version, author)]
pub struct StartupParams {
    #[clap(flatten)]
    pub tokio_runtime_params: TokioRuntimeParams,

    #[clap(flatten)]
    pub logging_params: LoggingParams,

    #[clap(short, long, default_value = "apply")]
    pub crds: ResourceDefinitionsStrategy,
}

#[derive(Debug, Default, Clone, ValueEnum)]
pub enum ResourceDefinitionsStrategy {
    /// Fail if the CRD's are missing. This might become the default in the future.
    #[clap(name = "fail-if-missing")]
    Fail,
    /// Apply the CRD's if they are missing. Forwards the CRD's if they are already present.
    /// This is the default behavior for now (at least during development).
    #[default]
    #[clap(name = "apply")]
    Apply,
    /// Do nothing bout the CRD's. Its to the administrator to ensure they are present and up to date.
    #[clap(name = "ignore")]
    Ignore,
}
