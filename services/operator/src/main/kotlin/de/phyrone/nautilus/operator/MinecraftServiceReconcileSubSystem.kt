package de.phyrone.nautilus.operator

import de.phyrone.nautilus.appcommons.Subsystem
import de.phyrone.nautilus.k8s.crds.v1alpha1.MinecraftService
import de.phyrone.nautilus.lib.k8s.*
import de.phyrone.nautilus.operator.resources.minecraftService
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.client.KubernetesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import kotlin.time.measureTime

class MinecraftServiceReconcileSubSystem : Subsystem {
    private val kubernetesClient by inject<KubernetesClient>()
    private val deployments by lazy { kubernetesClient.apps().deployments() }
    private val services by lazy { kubernetesClient.services() }
    private val statefulSets by lazy { kubernetesClient.apps().statefulSets() }

    override suspend fun runSubsystem() {
        val services = kubernetesClient.resources(MinecraftService::class.java)

        var generation = -1L
        runResourceJobs(services) {
            logger.info("[$namespace/$name] Tracking Started")
            try {
                resource
                    .distinctUntilChanged { old, new -> old.metadata.generation == new.metadata.generation }
                    .collect { resourceState ->
                        logger.debug("[{}/{}] Change Detected ({})", namespace, name, resourceState.metadata.generation)

                        logger.info("[{}/{}] Reconcile", namespace, name)
                        val reconcileTime = measureTime {
                            reconcile(resourceState)
                        }
                        logger.info(
                            "[{}/{}] Reconcile Complete ({})",
                            namespace,
                            name,
                            reconcileTime,
                        )
                    }
            } catch (e: ResourceDeletedException) {
                logger.info("[{}/{}] Deleted", namespace, name)
            } finally {
                logger.info("[{}/{}] Tracking Stopped", namespace, name)
            }
        }
    }

    suspend fun delete(
        minecraftService: MinecraftService,
    ) {
        withContext(Dispatchers.IO) {
            deployments
                .inNamespace(minecraftService.metadata.namespace)
                .withName(minecraftService.metadata.name)
                .delete()
            services
                .inNamespace(minecraftService.metadata.namespace)
                .withName(minecraftService.metadata.name)
                .delete()
            statefulSets
                .inNamespace(minecraftService.metadata.namespace)
                .withName(minecraftService.metadata.name)
                .delete()
        }
    }

    private suspend fun reconcile(
        resource: MinecraftService,
    ) {

        val updatedNetService = (withContext(Dispatchers.IO) {
            services
                .inNamespace(resource.metadata.namespace)
                .withName(resource.metadata.name)
                .get()
        } ?: Service()).edit()
            .minecraftService(resource)
            .editMetadata()
            .withManagedFields()
            .endMetadata()
            .build()
        withContext(Dispatchers.IO) {
            services.resource(updatedNetService).serverSideApply()
        }

        //TODO recreate if apply is not possible

        if (resource.spec.persistence?.enabled == true) {
            val updatedStatefulSet = (withContext(Dispatchers.IO) {
                deployments
                    .inNamespace(resource.metadata.namespace)
                    .withName(resource.metadata.name)
                    .delete()
                statefulSets
                    .inNamespace(resource.metadata.namespace)
                    .withName(resource.metadata.name)
                    .get()
            } ?: StatefulSet()).edit()
                .minecraftService(resource)
                .editMetadata()
                .withManagedFields()
                .endMetadata()
                .build()
            withContext(Dispatchers.IO) {
                statefulSets.resource(updatedStatefulSet).serverSideApply()
            }
        } else {
            val updatedDeployment = (withContext(Dispatchers.IO) {
                statefulSets
                    .inNamespace(resource.metadata.namespace)
                    .withName(resource.metadata.name)
                    .delete()
                deployments
                    .inNamespace(resource.metadata.namespace)
                    .withName(resource.metadata.name)
                    .get()
            } ?: Deployment()).edit()
                .minecraftService(resource)
                .editMetadata()
                .withManagedFields()
                .endMetadata()
                .build()
            withContext(Dispatchers.IO) {
                deployments.resource(updatedDeployment).serverSideApply()
            }
        }

    }


    companion object {
        private val logger = LoggerFactory.getLogger(MinecraftServiceReconcileSubSystem::class.java)
    }
}
