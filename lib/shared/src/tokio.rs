use clap::Args;
use error_stack::Report;
use tokio::runtime::Runtime;
use tracing::{info, instrument};

#[derive(Debug, Clone, Args)]
pub struct TokioRuntimeParams {
    #[clap(short, long="worker-threads",value_parser=Self::parse_workers, default_value_t = num_cpus::get().max(1),env= "WORKER_THREADS"
    )]
    pub workers: usize,
}
impl TokioRuntimeParams {
    fn parse_workers(input: &str) -> Result<usize, String> {
        clap_num::number_range(input, 1, usize::MAX)
    }
}

#[instrument(skip(params))]
pub fn init_tokio_runtime(
    params: &TokioRuntimeParams,
) -> error_stack::Result<Runtime, std::io::Error> {
    const RUNTIME_STACK_SIZE: usize = 32 * 1024 * 1024;
    match params.workers {
        0 => unreachable!(),
        1 => {
            info!("Initializing single-threaded tokio runtime");
            tokio::runtime::Builder::new_current_thread()
                .enable_all()
                .thread_stack_size(RUNTIME_STACK_SIZE)
                .build()
                .map_err(Report::new)
        }
        workers => {
            info!(
                "Initializing multi-threaded tokio runtime with {} workers",
                workers
            );
            tokio::runtime::Builder::new_multi_thread()
                .enable_all()
                .thread_stack_size(RUNTIME_STACK_SIZE)
                .worker_threads(workers)
                .thread_name("async-worker")
                .build()
                .map_err(Report::new)
        }
    }
}
