package de.phyrone.nautilus.operator.resources

import de.phyrone.nautilus.k8s.crds.v1alpha1.MinecraftServer
import de.phyrone.nautilus.k8s.crds.v1alpha1.MinecraftTemplate
import de.phyrone.nautilus.k8s.crds.v1alpha1.minecraftserverspec.install.Software
import de.phyrone.nautilus.k8s.crds.v1alpha1.minecraftserverspec.resources.Memory
import de.phyrone.nautilus.lib.k8s.DEFAULT_NAUTILUS_LABELS
import de.phyrone.nautilus.lib.k8s.LABEL_NAUTILUS_SERVICE
import de.phyrone.nautilus.lib.k8s.LABEL_NAUTILUS_SERVICE_CLASS
import de.phyrone.nautilus.lib.k8s.VALUE_NAUTILUS_SERVICE_CLASS_SERVER
import de.phyrone.nautilus.shared.parseMemorySize
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.DeploymentFluent
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategyFluent
import io.fabric8.kubernetes.api.model.apps.StatefulSetFluent
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategyFluent

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
const val ITZG_ENV_DEBUG_EXEC = "DEBUG_EXEC"
const val ITZG_ENV_SPIGET_RESOURCES = "SPIGET_RESOURCES"
const val ITZG_ENV_MODRINTH_PROJECTS = "MODRINTH_PROJECTS"

const val ITZH_ENV_MEMORY = "MEMORY"
const val ITZH_ENV_INIT_MEMORY = "INIT_MEMORY"
const val ITZH_ENV_MAX_MEMORY = "MAX_MEMORY"

const val ITZH_ENV_PROPS_ONLINE_MODE = "ONLINE_MODE"
const val ITZH_ENV_PROPS_SERVER_NAME = "SERVER_NAME"

const val PAPER_ENV_VELOCITY_SECRET = "PAPER_VELOCITY_SECRET"

const val VOLUME_NAME_SERVER_DATA = "server-data"

const val GENERAL_SERVICE_SERVER_PREFIX = "mc-server-"

@PublishedApi
internal inline fun <reified A : DeploymentStrategyFluent<A>> A.deploymentStrategy(minecraftService: MinecraftServer): A {
    val spec = minecraftService.spec.deploymentStrategy
    val type = spec?.type
    if (!type.isNullOrBlank()) {
        this.withType(type)
        val rollingUpdate = spec.rollingUpdate
        if (rollingUpdate != null) {
            this.withNewRollingUpdate().withMaxSurge(rollingUpdate.maxSurge)
                .withMaxUnavailable(rollingUpdate.maxUnavailable)
                .endRollingUpdate()
        }
    }
    return this
}

@PublishedApi
internal inline fun <reified A : StatefulSetUpdateStrategyFluent<A>> A.deploymentStrategy(minecraftService: MinecraftServer): A {
    val spec = minecraftService.spec.deploymentStrategy
    val type = spec?.type
    if (!type.isNullOrBlank()) {
        this.withType(type)
        val rollingUpdate = spec.rollingUpdate
        if (rollingUpdate != null) {
            this.withNewRollingUpdate().withMaxUnavailable(rollingUpdate.maxUnavailable).endRollingUpdate()
        }
    }
    return this
}

@PublishedApi
internal inline fun <reified A : ContainerFluent<A>> A.addVersionEnv(version: String): A =
    if (version.isUseLatestVersion()) {
        this
    } else {
        this.addNewEnv().withName(ITZG_ENV_SERVER_VERSION).withValue(version).endEnv()
    }

inline fun <reified A : ServiceFluent<A>> A.minecraftServer(minecraftService: MinecraftServer): A =
    this.editOrNewMetadata().withNamespace(minecraftService.metadata.namespace)
        .withName(GENERAL_SERVICE_SERVER_PREFIX + minecraftService.metadata.name).addToLabels(DEFAULT_NAUTILUS_LABELS)
        .withOwnerReferences(
            OwnerReferenceBuilder().withName(minecraftService.metadata.name).withApiVersion(minecraftService.apiVersion)
                .withKind(minecraftService.kind).withUid(minecraftService.metadata.uid).withController(true).build(),
        ).endMetadata().editOrNewSpec().withSelector<String, String>(
            mapOf(
                LABEL_NAUTILUS_SERVICE to minecraftService.metadata.name,
                LABEL_NAUTILUS_SERVICE_CLASS to VALUE_NAUTILUS_SERVICE_CLASS_SERVER,
            ),
        ).withType("ClusterIP").withClusterIP("None").withIpFamilyPolicy("PreferDualStack").withPorts(
            ServicePortBuilder().withName("minecraft").withPort(25565).withTargetPort(IntOrString(25565)).build(),
        ).endSpec()

inline fun <reified A : StatefulSetFluent<A>> A.minecraftServer(
    minecraftService: MinecraftServer,
    template: MinecraftTemplate? = null,
): A =
    this.editOrNewMetadata().withNamespace(minecraftService.metadata.namespace).withName(minecraftService.metadata.name)
        .addToLabels(DEFAULT_NAUTILUS_LABELS).withOwnerReferences(
            OwnerReferenceBuilder().withName(minecraftService.metadata.name).withApiVersion(minecraftService.apiVersion)
                .withKind(minecraftService.kind).withUid(minecraftService.metadata.uid).withController(true).build(),
        ).endMetadata().editOrNewSpec().withReplicas(minecraftService.spec.replicas).editOrNewSelector()
        .addToMatchLabels(
            mapOf(
                LABEL_NAUTILUS_SERVICE to minecraftService.metadata.name,
                LABEL_NAUTILUS_SERVICE_CLASS to VALUE_NAUTILUS_SERVICE_CLASS_SERVER,
            ),
        ).endSelector().withNewUpdateStrategy().deploymentStrategy(minecraftService).endUpdateStrategy()
        .editOrNewTemplate().minecraftServer(minecraftService, template, VOLUME_NAME_SERVER_DATA).endTemplate()
        .withVolumeClaimTemplates(
            PersistentVolumeClaimBuilder().editOrNewMetadata().withName(VOLUME_NAME_SERVER_DATA).endMetadata()
                .editOrNewSpec().withAccessModes("ReadWriteOnce")
                .withStorageClassName(minecraftService.spec.persistence?.storageClass).let {
                    val volumeSize = minecraftService.spec.persistence?.size
                    val requests =
                        volumeSize?.let { size ->
                            mapOf("storage" to Quantity(size))
                        }
                    it.editOrNewResources().withRequests<String, Quantity>(requests).endResources()
                }.endSpec().build(),
        ).endSpec()

inline fun <reified A : DeploymentFluent<A>> A.minecraftServer(
    minecraftService: MinecraftServer,
    template: MinecraftTemplate? = null,
): A =
    this.editOrNewMetadata().withNamespace(minecraftService.metadata.namespace).withName(minecraftService.metadata.name)
        .addToLabels(DEFAULT_NAUTILUS_LABELS).withOwnerReferences(
            OwnerReferenceBuilder().withName(minecraftService.metadata.name).withApiVersion(minecraftService.apiVersion)
                .withKind(minecraftService.kind).withUid(minecraftService.metadata.uid).withController(true).build(),
        ).endMetadata().editOrNewSpec().withReplicas(minecraftService.spec.replicas).withNewStrategy()
        .deploymentStrategy(minecraftService).endStrategy().editOrNewSelector().addToMatchLabels(
            mapOf(
                LABEL_NAUTILUS_SERVICE to minecraftService.metadata.name,
                LABEL_NAUTILUS_SERVICE_CLASS to VALUE_NAUTILUS_SERVICE_CLASS_SERVER,
            ),
        ).endSelector().editOrNewTemplate().minecraftServer(minecraftService, template).endTemplate().endSpec()

inline fun <reified A : PodTemplateSpecFluent<A>> A.minecraftServer(
    minecraftService: MinecraftServer,
    template: MinecraftTemplate? = null,
    persistentVolumeClaim: String? = null,
): A =
    this.editOrNewMetadata().withNamespace(minecraftService.metadata.namespace).addToLabels(DEFAULT_NAUTILUS_LABELS)
        .addToLabels(
            mapOf(
                // TODO Set type depeding on type (minecraft-server, bungee-proxy, velocity-proxy, geyser-proxy)
                // LABEL_K8S_COMPONENT to "minecraft-server",
                // TODO Set version of application or minecraft version
                // LABEL_K8S_VERSION to "1.20.0"
                LABEL_NAUTILUS_SERVICE to minecraftService.metadata.name,
                LABEL_NAUTILUS_SERVICE_CLASS to VALUE_NAUTILUS_SERVICE_CLASS_SERVER,
            ),
        ).endMetadata().editOrNewSpec().withSubdomain(GENERAL_SERVICE_SERVER_PREFIX + minecraftService.metadata.name)
        .withHostNetwork(false).withTerminationGracePeriodSeconds(90).withContainers(
            ContainerBuilder().withName("server").withTty(true).withStdin(true).withStdinOnce(false)
                .withImage(minecraftService.spec.image ?: ITZG_DOCKER_IMAGE) // TODO template image
                .withImagePullPolicy("IfNotPresent").addNewPort().withContainerPort(25565).withName("minecraft")
                .withProtocol("TCP").endPort().setMemoryResources(minecraftService.spec.resources?.memory)
                // TODO move env variables into own logic
                .addNewEnv().withName(ITZG_ENV_EULA).withValue("true").endEnv().addNewEnv()
                .withName(ITZG_ENV_SERVER_PORT)
                .withValue("25565").endEnv().addNewEnv().withName(ITZG_ENV_SERVER_ENABLE_RCON).withValue("false")
                .endEnv()
                .addNewEnv().withName(ITZH_ENV_EXEC_DIRECTLY).withValue("true").endEnv().addNewEnv()
                .withName(ITZG_ENV_ENABLE_AIKAR_FLAGS).withValue("true").endEnv().addNewEnv()
                .withName(ITZG_ENV_ENABLE_SIMD)
                .withValue("true").endEnv().addNewEnv().withName(ITZG_ENV_ENABLE_FLARE).withValue("true").endEnv()
                .addNewEnv().withName(ITZH_ENV_PROPS_ONLINE_MODE).withValue("false").endEnv().addNewEnv()
                .withName(ITZH_ENV_PROPS_SERVER_NAME).withNewValueFrom().withNewFieldRef()
                .withFieldPath("metadata.name")
                .endFieldRef().endValueFrom().endEnv().addNewEnv().withName(ITZG_ENV_DEBUG_EXEC).withValue("true")
                .endEnv()
                .also {
                    val spigotResources =
                        minecraftService.spec?.install?.spigot?.filterNotNull()
                            ?.joinToString(separator = ",") { it.toString() }
                    if (spigotResources != null) {
                        it.addNewEnv().withName(ITZG_ENV_SPIGET_RESOURCES)
                            .withValue(spigotResources).endEnv()
                    }
                }.also {
                    val modrinthProjects =
                        minecraftService.spec?.install?.modrinth?.filterNotNull()
                            ?.joinToString(separator = "\n") { it.replace("\n", "").trim() }
                    if (modrinthProjects != null) {
                        it.addNewEnv().withName(ITZG_ENV_MODRINTH_PROJECTS)
                            .withValue(modrinthProjects).endEnv()
                    }
                }.addSoftwareEnv(minecraftService.spec?.install?.software)
                // Health Checks
                .withNewReadinessProbe().withNewExec().withCommand(
                    "mc-monitor",
                    "status",
                    "--host",
                    "localhost",
                    "--port",
                    "25565",
                ).endExec().withInitialDelaySeconds(10).withPeriodSeconds(5).withFailureThreshold(10)
                .endReadinessProbe()
                .withNewStartupProbe().withInitialDelaySeconds(10).withPeriodSeconds(5).withFailureThreshold(60)
                .withNewExec().withCommand(
                    "mc-monitor",
                    "status",
                    "--host",
                    "localhost",
                    "--port",
                    "25565",
                ).endExec().endStartupProbe()
                // Persistent Volume Mount
                .let {
                    if (persistentVolumeClaim != null) {
                        it.addNewVolumeMount().withName(VOLUME_NAME_SERVER_DATA).withMountPath("/data").endVolumeMount()
                    } else {
                        it
                    }
                }.build(),
        ).endSpec()

/**
 * Default max heap size for Minecraft servers in bytes
 */
const val DEFAULT_MAX_HEAP = 2 * 1024 * 1024 * 1024L

@PublishedApi
internal inline fun <reified A : ContainerFluent<A>> A.setMemoryResources(spec: Memory?): A {
    if (spec != null) {
        val maxHeap =
            spec.maxHeap
                ?.let { max -> max.intVal?.toLong() ?: max.strVal?.let { parseMemorySize(it) } }?.coerceAtLeast(1024)
                ?: DEFAULT_MAX_HEAP.takeUnless { spec.maxHeap?.strVal?.isBlank() == true }

        val minHeap =
            maxHeap?.let { max ->
                spec.minHeap
                    ?.let { min -> min.intVal?.toLong() ?: min.strVal?.let { parseMemorySize(it, relativeTo = max) } }
                    ?.coerceAtMost(max)
                // use max memory except min memory is explicitly set with a blank value string
                    ?: maxHeap.takeUnless { spec.minHeap?.strVal?.isBlank() == true }
            }

        if (maxHeap != null) {
            val maxHeapKib = maxHeap / 1024
            val maxHeapKb = maxHeap / 1000
            val resources = editOrNewResources()
            addNewEnv()
                .withName(ITZH_ENV_MAX_MEMORY)
                .withValue("${maxHeapKb}K")
                .endEnv()

            if (minHeap != null) {
                val minHeapKib = minHeap / 1024
                val minHeapKb = minHeap / 1000
                addNewEnv()
                    .withName(ITZH_ENV_MEMORY)
                    .withValue("${minHeapKb}K")
                    .endEnv()
                resources
                    .addToRequests("memory", Quantity(minHeapKib.toString(), "Ki"))
            }

            if (spec.enableContainerLimit == true) {
                val jvmOverhead =
                    spec.jvmOverhead?.let { jvmOverhead ->
                        jvmOverhead.intVal?.toLong() ?: jvmOverhead.strVal?.let {
                            parseMemorySize(it, relativeTo = maxHeap)
                        }
                    }?.coerceAtLeast(0) ?: /* 25% of max heap */ (maxHeap / 4)

                val containerLimit = maxHeap + jvmOverhead
                val containerLimitKib = containerLimit / 1024
                resources.addToLimits("memory", Quantity(containerLimitKib.toString(), "Ki"))

                //overwrite
                if (minHeap == maxHeap)
                    resources.addToRequests("memory", Quantity(containerLimitKib.toString(), "Ki"))
            }

            resources.endResources()
        } else {
            addNewEnv()
                .withName(ITZH_ENV_MEMORY)
                .withValue("")
                .endEnv()
                .addNewEnv()
        }
    }

    return this
}

@PublishedApi
internal fun String.isUseLatestVersion(): Boolean = this.equals("latest", ignoreCase = true) || this.isBlank()

@PublishedApi
internal inline fun <reified A : ContainerFluent<A>> A.addSoftwareEnv(software: Software?): A =
    when {
        software?.paper != null ->
            addNewEnv().withName(ITZG_ENV_SERVER_TYPE)
                .withValue(ITZG_ENV_VALUE_SERVER_TYPE_PAPER)
                .endEnv().addVersionEnv(software.paper)

        software?.purpur != null ->
            addNewEnv().withName(ITZG_ENV_SERVER_TYPE)
                .withValue(ITZG_ENV_VALUE_SERVER_TYPE_PURPUR)
                .endEnv().addVersionEnv(software.purpur)

        software?.folia != null ->
            addNewEnv().withName(ITZG_ENV_SERVER_TYPE)
                .withValue(ITZG_ENV_VALUE_SERVER_TYPE_FOLIA)
                .endEnv().addVersionEnv(software.folia)

        else -> addNewEnv().withName(ITZG_ENV_SERVER_TYPE).withValue(ITZG_ENV_VALUE_SERVER_TYPE_PAPER).endEnv()
    }
