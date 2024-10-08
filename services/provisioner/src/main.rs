use std::fs;
use clap::Parser;
use nautilus_shared_lib::logging::{init_logger, LoggingParams};
use std::path::{Path, PathBuf};
use error_stack::{Report, ResultExt};
use git2::{Repository, RepositoryInitMode, RepositoryInitOptions};
use thiserror::Error;
use tracing::info;
use url::Url;
use crate::ProvisionerError::Io;

const REMOTE_NAME: &str = "origin";

#[derive(Debug, Clone, Parser)]
pub struct ProvisionerParams {
    /// The URL of the repository to clone or pull from
    #[clap(env = "REPOSITORY")]
    repository: Url,

    /// The branch to clone or pull from the repository
    /// If not provided, the default branch will be used
    #[clap(env = "BRANCH")]
    branch: Option<String>,

    /// The directory to upsert with the repository contents
    /// If not provided, the current working directory will be used
    #[clap(short, long, name = "DIR", env = "DATA_DIR")]
    data_dir: Option<PathBuf>,

    #[clap(flatten)]
    logging_params: LoggingParams,
}

#[derive(Debug, Error)]
#[error("An error occurred while provisioning the data directory")]
pub struct ProvisionerErrorRoot;

#[derive(Debug, Error)]
pub enum ProvisionerError {
    #[error("I/O error")]
    Io,

    #[error("git error")]
    Git,

    #[error("invalid state")]
    IllegalState,

    #[error("unknown error")]
    Unknown,
}


fn main() -> error_stack::Result<(), ProvisionerErrorRoot> {
    let params = ProvisionerParams::parse();
    init_logger(&params.logging_params);

    let data_dir = params.data_dir.unwrap_or_else(|| {
        PathBuf::from(".")
            .canonicalize()
            .unwrap_or_else(|e| PathBuf::from("."))
    });
    info!("data directory: {:?}", data_dir);
    if let Some(branch) = params.branch {
        info!("repository: {} with branch: {}", params.repository, branch);
    } else {
        info!("repository: {} with default branch", params.repository);
    }
    let repo = get_git_repo(&data_dir).change_context(ProvisionerErrorRoot)?;

    //checkout branch
    repo.remote_set_url(REMOTE_NAME, &params.repository.to_string())
        .change_context(ProvisionerError::Git)
        .change_context(ProvisionerErrorRoot)?;
    let remote = get_or_create_remote(repo).change_context(ProvisionerErrorRoot)?;


    //if is not initialized yet clone repo
    if repo.is_empty()
        .change_context(ProvisionerError::Git)
        .change_context(ProvisionerErrorRoot)? {
        info!("repo is empty");
        //clone repo

    } else {
        //set remote url for current branch
        let branch = repo.head()
            .change_context(ProvisionerError::Git)
            .change_context(ProvisionerErrorRoot)?;
        info!("current branch: {:?}", branch.name());
    }


    Ok(())
}

//data dir exists and a git repo is present but not its up to date
fn get_git_repo(
    data_dir: &Path,
) -> error_stack::Result<git2::Repository, ProvisionerError> {
    let mut init_options = RepositoryInitOptions::new();
    init_options.bare(false)
        .mkpath(true)
        .no_dotgit_dir(false)
        .no_reinit(false);
    let repo = Repository::init_opts(&data_dir, &init_options)
        .change_context(ProvisionerError::Io)?;
    Ok(repo)
}


fn get_or_create_remote(
    repository: Repository,
) -> error_stack::Result<git2::Remote, ProvisionerError> {
    let remote = repository.find_remote(REMOTE_NAME)
        .change_context(ProvisionerError::Git)?;
    Ok(remote)
}