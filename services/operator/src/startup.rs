use clap::{Parser, ValueEnum};
use clap_verbosity::{InfoLevel, Verbosity};

#[derive(Debug, Clone, Parser)]
#[clap(version)]
pub struct StartupParams {
    #[clap(short, long)]
    pub namespace: Option<String>,

    #[clap(short, long, default_value = "apply")]
    pub crds: CrdsStrategy,

    #[clap(flatten)]
    pub verbosity: Verbosity<InfoLevel>,
}

#[derive(Default, Debug, Clone, ValueEnum)]
pub enum CrdsStrategy {
    /// Noop, do not do anything with the CRDs.
    None,
    /// Validate the CRDs on startup, fail bootstrap if they are not valid.
    Validate,
    #[default]
    /// Apply the CRDs on startup, fail bootstrap if not successful.
    Apply,
}
