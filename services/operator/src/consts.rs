pub const LABEL_K8S_APP_NAME: &str = "app.kubernetes.io/name";
pub const LABEL_K8S_APP_INSTANCE: &str = "app.kubernetes.io/instance";
pub const LABEL_K8S_APP_VERSION: &str = "app.kubernetes.io/version";
pub const LABEL_K8S_APP_COMPONENT: &str = "app.kubernetes.io/component";
pub const LABEL_K8S_APP_PART_OF: &str = "app.kubernetes.io/part-of";
pub const LABEL_K8S_APP_MANAGED_BY: &str = "app.kubernetes.io/managed-by";

pub const LABEL_NAUTILUS_TYPE: &str = "nautilus.phyrone.de/type";
pub const LABEL_NAUTILUS_GROUP: &str = "nautilus.phyrone.de/group";
pub const LABEL_NAUTILUS_ON_STOP: &str = "nautilus.phyrone.de/on-stop";

pub const CONTAINER_NAME_SERVER: &str = "minecraft";

pub const LABEL_VALUE_NAUTILUS_TYPE_SERVER: &str = "server";
pub const LABEL_VALUE_NAUTILUS_TYPE_PROXY: &str = "proxy";

pub const LABEL_VALUE_NAUTILUS_ON_STOP_KEEP: &str = "keep";
pub const LABEL_VALUE_NAUTILUS_ON_STOP_DELETE: &str = "delete";

pub const FIELD_MANAGER_NAME: &str = "nautilus-minecraft-operator";

pub const IMAGE_MINECRAFT_SERVER: &str = "itzg/minecraft-server";
pub const IMAGE_MINECRAFT_PROXY: &str = "phyrone/minecraft-proxy";
pub const IMAGE_NAUTILUS_PROVISIONER: &str = "ghcr.io/phyrone/nautilus/provisioner";
pub const IMAGE_SQUID_PROXY: &str = "ubuntu/squid";

pub const VOLUME_NAME_MINECRAFT_SERVER: &str = "minecraft-server";
pub const VOLUME_NAME_TEMPLATE: &str = "template";

pub const PERSISTENT_VOLUME_CLAIM_NAME_PREFIX_MINECRAFT_SERVER: &str = "mc-server-";
pub const PERSISTENT_VOLUME_CLAIM_NAME_PREFIX_MINECRAFT_SERVER_BACKUP: &str = "mc-backup-";

pub const SQUID_PROXY_PORT: i32 = 3128;
pub const SQUID_PROXY_NAME: &str = "squid-proxy";