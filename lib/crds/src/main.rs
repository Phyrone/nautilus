use k8s_openapi::api::core::v1::Service;
use kube::CustomResourceExt;
use nautilus_shared_crds::service::MinecraftService;
use nautilus_shared_crds::template::Template;

/// This is a simple utility which prints the CRDs to stdout (as YAML).
/// The specs are seperated by yaml document separators (`---`) including a trailing separator.
/// It may be used during development and build processes.
/// Example usage:
/// ```sh
/// cargo run --bin crds > crds.yaml
/// ```
fn main() {
    let crds = vec![
        MinecraftService::crd(),
        Template::crd(),
    ];
    for crd in crds {
        let yaml = serde_yml::to_string(&crd)
            .expect("Failed to serialize CRD");
        println!("{}", yaml);
        println!("---");
    }
}