pub mod service;
pub mod template;

use k8s_openapi::apiextensions_apiserver::pkg::apis::apiextensions::v1::CustomResourceDefinition;
use kube::api::{DeleteParams, Patch, PatchParams, Preconditions};
use kube::{Api, Client, CustomResource, CustomResourceExt};
use tokio::{try_join, join};
use crate::service::MinecraftService;
use crate::template::Template;

async fn apply_crd<T>(api: &Api<CustomResourceDefinition>) -> kube::Result<CustomResourceDefinition>
where
    T: CustomResourceExt,
{
    let definition = T::crd();
    let apply = Patch::Apply(&definition);
    api.patch(T::crd_name(), &PatchParams::apply(T::crd_name()), &apply).await
}

async fn delte_crd<T>(
    api: &Api<CustomResourceDefinition>,
) where
    T: CustomResourceExt,
{
    let _ = api.delete(T::crd_name(), &DeleteParams::background()).await;
}

macro_rules! crd_impl {
    ($($crd:ident),+) => {
        pub async fn delete_crds(client: Client) {
            let definition = Api::<CustomResourceDefinition>::all(client);
            join!($(delte_crd::<$crd>(&definition),)+);
        }
        pub async fn apply_crds(client: Client) -> kube::Result<()> {
            let definition = Api::<CustomResourceDefinition>::all(client);
            try_join!( $(apply_crd::<$crd>(&definition),)+ )?;
            Ok(())
        }
    };
}

crd_impl!(
    MinecraftService,
    Template
);
