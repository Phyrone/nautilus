package de.phyrone.nautilus.operator.resources

import de.phyrone.nautilus.k8s.crds.v1alpha1.MinecraftService
import de.phyrone.nautilus.k8s.crds.v1alpha1.MinecraftTemplate
import de.phyrone.nautilus.lib.k8s.DEFAULT_NAUTILUS_LABELS
import de.phyrone.nautilus.lib.k8s.LABEL_NAUTILUS_SERVICE
import de.phyrone.nautilus.lib.k8s.LABEL_NAUTILUS_SERVICE_CLASS
import de.phyrone.nautilus.lib.k8s.VALUE_NAUTILUS_SERVICE_CLASS_SERVER
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.DeploymentFluent
import io.fabric8.kubernetes.api.model.apps.StatefulSetFluent


inline fun <reified A : ServiceFluent<A>> A.minecraftService(
    minecraftService: MinecraftService,
): A = this.editOrNewMetadata()
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
            .build()
    )
    .endMetadata()
    .editOrNewSpec()
    .withSelector<String, String>(
        mapOf(
            LABEL_NAUTILUS_SERVICE to minecraftService.metadata.name,
            LABEL_NAUTILUS_SERVICE_CLASS to VALUE_NAUTILUS_SERVICE_CLASS_SERVER
        )
    )
    .withType("ClusterIP")
    .withClusterIP("None")
    .withIpFamilyPolicy("PreferDualStack")
    .withPorts(
        ServicePortBuilder()
            .withName("minecraft")
            .withPort(25565)
            .withTargetPort(IntOrString(25565))
            .build()
    )
    .endSpec()

inline fun <reified A : StatefulSetFluent<A>> A.minecraftService(
    minecraftService: MinecraftService,
    template: MinecraftTemplate? = null
): A = this
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
            .build()
    )
    .endMetadata()
    .editOrNewSpec()
    .withReplicas(minecraftService.spec.replicas)
    .editOrNewSelector()
    .addToMatchLabels(
        mapOf(
            LABEL_NAUTILUS_SERVICE to minecraftService.metadata.name,
            LABEL_NAUTILUS_SERVICE_CLASS to VALUE_NAUTILUS_SERVICE_CLASS_SERVER
        )
    )
    .endSelector()
    .editOrNewTemplate()
    .minecraftService(minecraftService, template)
    .endTemplate()
    .endSpec()


inline fun <reified A : DeploymentFluent<A>> A.minecraftService(
    minecraftService: MinecraftService,
    template: MinecraftTemplate? = null
): A = this
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
            .build()
    )
    .endMetadata()
    .editOrNewSpec()
    .withReplicas(minecraftService.spec.replicas)
    .editOrNewSelector()
    .addToMatchLabels(
        mapOf(
            LABEL_NAUTILUS_SERVICE to minecraftService.metadata.name,
            LABEL_NAUTILUS_SERVICE_CLASS to VALUE_NAUTILUS_SERVICE_CLASS_SERVER
        )
    )
    .endSelector()
    .editOrNewTemplate()
    .minecraftService(minecraftService, template)
    .endTemplate()
    .endSpec()

inline fun <reified A : PodTemplateSpecFluent<A>> A.minecraftService(
    minecraftService: MinecraftService,
    template: MinecraftTemplate? = null
): A = this
    .editOrNewMetadata()
    .withNamespace(minecraftService.metadata.namespace)
    .addToLabels(DEFAULT_NAUTILUS_LABELS)
    .addToLabels(
        mapOf(
            //TODO Set type depeding on type (minecraft-server, bungee-proxy, velocity-proxy, geyser-proxy)
            //LABEL_K8S_COMPONENT to "minecraft-server",
            //TODO Set version of application or minecraft version
            //LABEL_K8S_VERSION to "1.20.0"
            LABEL_NAUTILUS_SERVICE to minecraftService.metadata.name,
            LABEL_NAUTILUS_SERVICE_CLASS to VALUE_NAUTILUS_SERVICE_CLASS_SERVER
        )
    )
    .endMetadata()
    .editOrNewSpec()
    .withHostNetwork(false)
    .withContainers(
        ContainerBuilder()
            .withName("minecraft")
            .withTty(true)
            .withStdin(true)
            .withStdinOnce(false)
            .withImage(minecraftService.spec.image ?: "itzg/minecraft-server") //TODO template image
            .withImagePullPolicy("IfNotPresent")
            .addNewPort()
            .withContainerPort(25565)
            .withName("minecraft")
            .withProtocol("TCP")
            .endPort()
            .addNewPort()
            .withContainerPort(25575)
            .withName("rcon")
            .withProtocol("TCP")
            .endPort()
            .withEnv(
                EnvVarBuilder()
                    .withName("EULA")
                    .withValue("true")
                    .build(),
                EnvVarBuilder()
                    .withName("PORT")
                    .withValue("25565")
                    .build(),
                EnvVarBuilder()
                    .withName("RCON_PORT")
                    .withValue("25575")
                    .build(),
            )
            .withNewReadinessProbe()
            .withNewExec()
            .withCommand(
                "mc-monitor",
                "status",
                "--host",
                "localhost",
                "--port",
                "25565"
            )
            .endExec()
            .withInitialDelaySeconds(30)
            .withPeriodSeconds(5)
            .withFailureThreshold(10)
            .endReadinessProbe()
            .build()
    )
    .endSpec()

