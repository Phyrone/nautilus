package de.phyrone.nautilus.lib.k8s

const val LABEL_PREFIX = "nautilus.phyrone.de"

/**
 * The name of the application.
 * Example: `mysql`
 * More at [Recommended Labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/common-labels/)
 */
const val LABEL_K8S_NAME = "app.kubernetes.io/name"

/**
 * A unique name identifying the instance of an application.
 * Example: `mysql-abcxyz`
 * More at [Recommended Labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/common-labels/)
 */
const val LABEL_K8S_INSTANCE = "app.kubernetes.io/instance"

/**
 * The current version of the application (e.g., a SemVer 1.0, revision hash, etc.)
 * Example: `5.7.21`
 * More at [Recommended Labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/common-labels/)
 */
const val LABEL_K8S_VERSION = "app.kubernetes.io/version"

/**
 * The component within the architecture.
 * Example: `database`
 * More at [Recommended Labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/common-labels/)
 */
const val LABEL_K8S_COMPONENT = "app.kubernetes.io/component"

/**
 * The name of a higher level application this one is part of.
 * Example: `wordpress`
 * More at [Recommended Labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/common-labels/)
 */
const val LABEL_K8S_PART_OF = "app.kubernetes.io/part-of"

/**
 * The tool being used to manage the operation of an application.
 * Example: `helm`
 * More at [Recommended Labels](https://kubernetes.io/docs/concepts/overview/working-with-objects/common-labels/)
 */
const val LABEL_K8S_MANAGED_BY = "app.kubernetes.io/managed-by"

/**
 * The used to identify which kind of service the resource is part of.
 * Example: `mineraft-service`, `minecraft-template`
 */
const val LABEL_NAUTILUS_SERVICE_CLASS = "$LABEL_PREFIX/service-class"

/**
 * The name of the minecraft service. Which manages the pod/deployment/statefulset/service etc.
 * Example: `lobby`
 */
const val LABEL_NAUTILUS_SERVICE = "$LABEL_PREFIX/service"

/**
 * The managed by value for the Nautilus operator.
 */
const val VALUE_MANAGED_BY_NAUTILUS = "Nautilus"

const val VALUE_PART_OF_MINECRAFT = "minecraft"

/**
 * Includes all minecraft services like servers, proxies etc.
 */
const val VALUE_NAUTILUS_SERVICE_CLASS_SERVER = "minecraft-service"

const val ANNOTATION_POD_DELETION_COST = "controller.kubernetes.io/pod-deletion-cost"
const val ANNOTATION_DEFAULT_CONTAINER = "kubectl.kubernetes.io/default-container"

val DEFAULT_NAUTILUS_LABELS =
    mapOf(
        LABEL_K8S_MANAGED_BY to VALUE_MANAGED_BY_NAUTILUS,
        LABEL_K8S_PART_OF to VALUE_PART_OF_MINECRAFT,
    )
