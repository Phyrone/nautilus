pub mod service;
pub mod template;

use crate::service::MinecraftService;
use crate::template::MinecraftTemplate;
use k8s_openapi::apiextensions_apiserver::pkg::apis::apiextensions::v1::CustomResourceDefinition;
use kube::api::{DeleteParams, Patch, PatchParams, Preconditions};
use kube::{Api, Client, CustomResource, CustomResourceExt};
use tokio::task::JoinSet;
use tokio::{join, try_join};

async fn crd_exists<T>(api: &Api<CustomResourceDefinition>) -> kube::Result<bool>
where
    T: CustomResourceExt,
{
    api.get_opt(T::crd_name()).await.map(|res| res.is_some())
}

async fn apply_crd<T>(api: &Api<CustomResourceDefinition>) -> kube::Result<CustomResourceDefinition>
where
    T: CustomResourceExt,
{
    let definition = T::crd();
    let apply = Patch::Apply(&definition);
    api.patch(T::crd_name(), &PatchParams::apply(T::crd_name()), &apply).await
}

async fn delte_crd<T>(api: &Api<CustomResourceDefinition>)
where
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

        pub async fn all_crds_exist(client: Client) -> kube::Result<bool> {
            let definition = Api::<CustomResourceDefinition>::all(client);
            let mut joins = JoinSet::new();
            $(let definition_c = definition.clone();joins.spawn(async move {crd_exists::<$crd>(&definition_c).await});)+
            while let Some(res) = joins.join_next().await {if let Ok(res) = res {if !res? {return Ok(false);}}else {continue;}}
            Ok(true)
        }
    };
}

crd_impl!(MinecraftService, MinecraftTemplate);
