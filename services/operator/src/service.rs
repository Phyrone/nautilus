use kube::Client;
use tokio_util::sync::CancellationToken;

pub struct ServiceReconciler {
    client: Client,
}

impl ServiceReconciler {
    
    /// Run the service reconciler
    pub async fn run(
        shutdown: CancellationToken
    ) {
        
        
    }
}