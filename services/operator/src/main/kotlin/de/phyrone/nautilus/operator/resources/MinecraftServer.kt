package de.phyrone.nautilus.operator.resources

import de.phyrone.nautilus.k8s.crds.v1alpha1.MinecraftServer
import de.phyrone.nautilus.k8s.crds.v1alpha1.MinecraftTemplate
import de.phyrone.nautilus.k8s.crds.v1alpha1.minecraftserverspec.install.Software
import de.phyrone.nautilus.lib.k8s.DEFAULT_NAUTILUS_LABELS
import de.phyrone.nautilus.lib.k8s.LABEL_NAUTILUS_SERVICE
import de.phyrone.nautilus.lib.k8s.LABEL_NAUTILUS_SERVICE_CLASS
import de.phyrone.nautilus.lib.k8s.VALUE_NAUTILUS_SERVICE_CLASS_SERVER
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.DeploymentFluent
import io.fabric8.kubernetes.api.model.apps.StatefulSetFluent

const val ITZG_DOCKER_IMAGE = "itzg/minecraft-server"
const val ITZG_ENV_EULA = "EULA"
const val ITZG_ENV_SERVER_TYPE = "TYPE"
const val ITZG_ENV_VALUE_SERVER_TYPE_PAPER = "PAPER"
const val ITZG_ENV_VALUE_SERVER_TYPE_PURPUR = "PURPUR"
const val ITZG_ENV_VALUE_SERVER_TYPE_FOLIA = "FOLIA"
const val ITZG_ENV_SERVER_VERSION = "VERSION"
const val ITZG_ENV_SERVER_PORT = "PORT"
const val ITZG_ENV_SERVER_ENABLE_RCON = "ENABLE_RCON"
const val ITZH_ENV_EXEC_DIRECTLY = "EXEC_DIRECTLY"
const val ITZG_ENV_ENABLE_AIKAR_FLAGS = "USE_AIKAR_FLAGS"
const val ITZG_ENV_ENABLE_SIMD = "USE_SIMD_FLAGS"
const val ITZG_ENV_ENABLE_FLARE = "USE_FLARE_FLAGS"
const val ITZG_ENV_JVM_OPTS = "JVM_OPTS"


const val ENV_VALUE_EXTRA_JVM_OPTS = ""//"-Djline.terminal=unix -Djline.terminal.force=true"

const val PAPER_ENV_VELOCITY_SECRET = "PAPER_VELOCITY_SECRET"

@PublishedApi
internal fun String.isUseLatestVersion(): Boolean = this.equals("latest", ignoreCase = true) || this.isBlank()

@PublishedApi
internal inline fun <reified A : ContainerFluent<A>> A.addSoftwareEnv(
    software: Software?
): A = when {
    software?.paper != null -> addNewEnv()
        .withName(ITZG_ENV_SERVER_TYPE)
        .withValue(ITZG_ENV_VALUE_SERVER_TYPE_PAPER)
        .endEnv()
        .addVersionEnv(software.paper)

    software?.purpur != null -> addNewEnv()
        .withName(ITZG_ENV_SERVER_TYPE)
        .withValue(ITZG_ENV_VALUE_SERVER_TYPE_PURPUR)
        .endEnv()
        .addVersionEnv(software.purpur)

    software?.folia != null -> addNewEnv()
        .withName(ITZG_ENV_SERVER_TYPE)
        .withValue(ITZG_ENV_VALUE_SERVER_TYPE_FOLIA)
        .endEnv()
        .addVersionEnv(software.folia)

    else -> addNewEnv()
        .withName(ITZG_ENV_SERVER_TYPE)
        .withValue(ITZG_ENV_VALUE_SERVER_TYPE_PAPER)
        .endEnv()
}

@PublishedApi
internal inline fun <reified A : ContainerFluent<A>> A.addVersionEnv(
    version: String
): A = if (version.isUseLatestVersion()) this
else this.addNewEnv()
    .withName(ITZG_ENV_SERVER_VERSION)
    .withValue(version)
    .endEnv()

inline fun <reified A : ServiceFluent<A>> A.minecraftServer(minecraftService: MinecraftServer): A =
    this.editOrNewMetadata()
        .withNamespace(minecraftService.metadata.namespace)
        .withName("nautilus-server-${minecraftService.metadata.name}")
        .addToLabels(DEFAULT_NAUTILUS_LABELS)
        .withOwnerReferences(
            OwnerReferenceBuilder()
                .withName(minecraftService.metadata.name)
                .withApiVersion(minecraftService.apiVersion)
                .withKind(minecraftService.kind)
                .withUid(minecraftService.metadata.uid)
                .withController(true)
                .build(),
        )
        .endMetadata()
        .editOrNewSpec()
        .withSelector<String, String>(
            mapOf(
                LABEL_NAUTILUS_SERVICE to minecraftService.metadata.name,
                LABEL_NAUTILUS_SERVICE_CLASS to VALUE_NAUTILUS_SERVICE_CLASS_SERVER,
            ),
        )
        .withType("ClusterIP")
        .withClusterIP("None")
        .withIpFamilyPolicy("PreferDualStack")
        .withPorts(
            ServicePortBuilder()
                .withName("minecraft")
                .withPort(25565)
                .withTargetPort(IntOrString(25565))
                .build(),
        )
        .endSpec()

inline fun <reified A : StatefulSetFluent<A>> A.minecraftServer(
    minecraftService: MinecraftServer,
    template: MinecraftTemplate? = null,
): A =
    this
        .editOrNewMetadata()
        .withNamespace(minecraftService.metadata.namespace)
        .withName(minecraftService.metadata.name)
        .addToLabels(DEFAULT_NAUTILUS_LABELS)
        .withOwnerReferences(
            OwnerReferenceBuilder()
                .withName(minecraftService.metadata.name)
                .withApiVersion(minecraftService.apiVersion)
                .withKind(minecraftService.kind)
                .withUid(minecraftService.metadata.uid)
                .withController(true)
                .build(),
        )
        .endMetadata()
        .editOrNewSpec()
        .withReplicas(minecraftService.spec.replicas)
        .editOrNewSelector()
        .addToMatchLabels(
            mapOf(
                LABEL_NAUTILUS_SERVICE to minecraftService.metadata.name,
                LABEL_NAUTILUS_SERVICE_CLASS to VALUE_NAUTILUS_SERVICE_CLASS_SERVER,
            ),
        )
        .endSelector()
        .editOrNewTemplate()
        .minecraftServer(minecraftService, template)
        .endTemplate()
        .endSpec()

inline fun <reified A : DeploymentFluent<A>> A.minecraftServer(
    minecraftService: MinecraftServer,
    template: MinecraftTemplate? = null,
): A =
    this
        .editOrNewMetadata()
        .withNamespace(minecraftService.metadata.namespace)
        .withName(minecraftService.metadata.name)
        .addToLabels(DEFAULT_NAUTILUS_LABELS)
        .withOwnerReferences(
            OwnerReferenceBuilder()
                .withName(minecraftService.metadata.name)
                .withApiVersion(minecraftService.apiVersion)
                .withKind(minecraftService.kind)
                .withUid(minecraftService.metadata.uid)
                .withController(true)
                .build(),
        )
        .endMetadata()
        .editOrNewSpec()
        .withReplicas(minecraftService.spec.replicas)
        .editOrNewSelector()
        .addToMatchLabels(
            mapOf(
                LABEL_NAUTILUS_SERVICE to minecraftService.metadata.name,
                LABEL_NAUTILUS_SERVICE_CLASS to VALUE_NAUTILUS_SERVICE_CLASS_SERVER,
            ),
        )
        .endSelector()
        .editOrNewTemplate()
        .minecraftServer(minecraftService, template)
        .endTemplate()
        .endSpec()

inline fun <reified A : PodTemplateSpecFluent<A>> A.minecraftServer(
    minecraftService: MinecraftServer,
    template: MinecraftTemplate? = null,
): A =
    this
        .editOrNewMetadata()
        .withNamespace(minecraftService.metadata.namespace)
        .addToLabels(DEFAULT_NAUTILUS_LABELS)
        .addToLabels(
            mapOf(
                // TODO Set type depeding on type (minecraft-server, bungee-proxy, velocity-proxy, geyser-proxy)
                // LABEL_K8S_COMPONENT to "minecraft-server",
                // TODO Set version of application or minecraft version
                // LABEL_K8S_VERSION to "1.20.0"
                LABEL_NAUTILUS_SERVICE to minecraftService.metadata.name,
                LABEL_NAUTILUS_SERVICE_CLASS to VALUE_NAUTILUS_SERVICE_CLASS_SERVER,
            ),
        )
        .endMetadata()
        .editOrNewSpec()
        .withHostNetwork(false)
        .withTerminationGracePeriodSeconds(300)
        .withContainers(
            ContainerBuilder()
                .withName("server")
                .withTty(true)
                .withStdin(true)
                .withStdinOnce(false)
                .withImage(minecraftService.spec.image ?: ITZG_DOCKER_IMAGE) // TODO template image
                .withImagePullPolicy("IfNotPresent")
                .addNewPort()
                .withContainerPort(25565)
                .withName("minecraft")
                .withProtocol("TCP")
                .endPort()
                .addNewEnv()
                .withName(ITZG_ENV_EULA)
                .withValue("true")
                .endEnv()
                .addNewEnv()
                .withName(ITZG_ENV_SERVER_PORT)
                .withValue("25565")
                .endEnv()
                .addNewEnv()
                .withName(ITZG_ENV_SERVER_ENABLE_RCON)
                .withValue("false")
                .endEnv()
                .addNewEnv()
                .withName(ITZH_ENV_EXEC_DIRECTLY)
                .withValue("true")
                .endEnv()
                .addNewEnv()
                .withName(ITZG_ENV_ENABLE_AIKAR_FLAGS)
                .withValue("true")
                .endEnv()
                .addNewEnv()
                .withName(ITZG_ENV_ENABLE_SIMD)
                .withValue("true")
                .endEnv()
                .addNewEnv()
                .withName(ITZG_ENV_ENABLE_FLARE)
                .withValue("true")
                .endEnv()
                .addNewEnv()
                .withName(ITZG_ENV_JVM_OPTS)
                .withValue(
                    ENV_VALUE_EXTRA_JVM_OPTS
                    //TODO allow to set jvm opts
                    //"${ENV_VALUE_EXTRA_JVM_OPTS} ${minecraftService.spec.jvmOpts ?: ""}"
                )
                .endEnv()
                .addSoftwareEnv(minecraftService.spec?.install?.software)
                // Health Checks
                .withNewReadinessProbe()
                .withNewExec()
                .withCommand(
                    "mc-monitor",
                    "status",
                    "--host",
                    "localhost",
                    "--port",
                    "25565",
                )
                .endExec()
                .withInitialDelaySeconds(10)
                .withPeriodSeconds(5)
                .withFailureThreshold(10)
                .endReadinessProbe()
                .withNewStartupProbe()
                .withInitialDelaySeconds(10)
                .withPeriodSeconds(5)
                .withFailureThreshold(60)
                .withNewExec()
                .withCommand(
                    "mc-monitor",
                    "status",
                    "--host",
                    "localhost",
                    "--port",
                    "25565",
                )
                .endExec()
                .endStartupProbe()
                .build(),
        )
        .endSpec()
