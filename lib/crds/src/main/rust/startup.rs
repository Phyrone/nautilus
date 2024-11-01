use clap::{Args, Parser, Subcommand};
use std::path::PathBuf;

#[derive(Debug, Clone, Parser)]
pub struct StartupParams {
    #[clap(subcommand)]
    pub subcommand: MainInstruction,
}

#[derive(Debug, Clone, Subcommand)]
pub enum MainInstruction {
    Export(ExportInstruction),
    /// Print all custom resource definitions into stdout
    Print,
    Apply,
    Delete,
}
#[derive(Debug, Clone, Args)]
pub struct ExportInstruction {
    /// Directory to export the CRDs into to or the file name if '--single-file' is set
    #[clap(short, long)]
    pub output: Option<PathBuf>,

    /// Export all CRDs into a single file instead of multiple ones
    #[clap(short, long)]
    pub single_file: bool,
}
