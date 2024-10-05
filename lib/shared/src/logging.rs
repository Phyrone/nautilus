use clap::Args;
use clap_verbosity::{InfoLevel, LevelFilter};
use tracing::instrument::WithSubscriber;
use tracing::subscriber;
use tracing_chrome::TraceStyle;
use tracing_subscriber::prelude::*;
use tracing_subscriber::util::SubscriberInitExt;

#[derive(Debug, Clone, Args)]
pub struct LoggingParams {
    #[clap(flatten)]
    pub verbosity: clap_verbosity::Verbosity<InfoLevel>,
}

pub fn init_logger(loggin_params: &LoggingParams) {
    let level = match loggin_params.verbosity.log_level_filter() {
        LevelFilter::Off => tracing::level_filters::LevelFilter::OFF,
        LevelFilter::Error => tracing::level_filters::LevelFilter::ERROR,
        LevelFilter::Warn => tracing::level_filters::LevelFilter::WARN,
        LevelFilter::Info => tracing::level_filters::LevelFilter::INFO,
        LevelFilter::Debug => tracing::level_filters::LevelFilter::DEBUG,
        LevelFilter::Trace => tracing::level_filters::LevelFilter::TRACE,
    };

    tracing_subscriber::fmt()
        .compact()
        .with_max_level(level)
        .with_ansi(true)
        .with_thread_names(true)
        .with_target(false)
        .with_line_number(false)
        .with_thread_ids(false)
        .with_writer(std::io::stdout)
        .init();
}
