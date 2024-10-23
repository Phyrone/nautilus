package de.phyrone.nautilus.operator

import de.phyrone.nautilus.appcommons.AppID
import de.phyrone.nautilus.appcommons.Subsystem
import de.phyrone.nautilus.k8s.crds.v1alpha1.MinecraftServer
import de.phyrone.nautilus.lib.k8s.*
import de.phyrone.nautilus.operator.resources.minecraftService
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.client.KubernetesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.slf4j.LoggerFactory
import kotlin.time.measureTime

class MinecraftServersSubsystem : Subsystem {
    private val kubernetesClient by inject<KubernetesClient>()
    private val deployments by lazy { kubernetesClient.apps().deployments() }
    private val netServices by lazy { kubernetesClient.services() }
    private val statefulSets by lazy { kubernetesClient.apps().statefulSets() }
    private val appID by inject<String>(named<AppID>())

    override suspend fun runSubsystem() {
        val services =
            kubernetesClient
                .resources(MinecraftServer::class.java)

        runResourceWatchers(services) { minecraftServerStateFlow ->
            coroutineScope {
                val mutex =
                    LeaderElector(
                        kubernetesClient,
                        namespace,
                        "nautilus-mcserver-$name",
                        appID,
                    )
                launch {
                    mutex.withLeadership {
                        logger.info("[{}/{}] Tracking Started", namespace, name)
                        try {
                            minecraftServerStateFlow
                                .distinctUntilChanged { old, new -> old.metadata.generation == new.metadata.generation }
                                .collect { resourceState ->
                                    logger.debug(
                                        "[{}/{}] Change Detected ({})",
                                        namespace,
                                        name,
                                        resourceState.metadata.generation,
                                    )

                                    logger.info("[{}/{}] Reconcile", namespace, name)
                                    val reconcileTime =
                                        measureTime {
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
                mutex.run()
            }
        }
    }

    suspend fun delete(MinecraftServer: MinecraftServer) {
        withContext(Dispatchers.IO) {
            deployments
                .inNamespace(MinecraftServer.metadata.namespace)
                .withName(MinecraftServer.metadata.name)
                .delete()
            netServices
                .inNamespace(MinecraftServer.metadata.namespace)
                .withName(MinecraftServer.metadata.name)
                .delete()
            statefulSets
                .inNamespace(MinecraftServer.metadata.namespace)
                .withName(MinecraftServer.metadata.name)
                .delete()
        }
    }

    private suspend fun reconcile(minecraftServer: MinecraftServer) {
        val updatedNetService =
            (
                withContext(Dispatchers.IO) {
                    netServices
                        .inNamespace(minecraftServer.metadata.namespace)
                        .withName(minecraftServer.metadata.name)
                        .get()
                } ?: Service()
            ).edit()
                .minecraftService(minecraftServer)
                .editMetadata()
                .withManagedFields()
                .endMetadata()
                .build()
        withContext(Dispatchers.IO) {
            netServices.resource(updatedNetService).serverSideApply()
        }

        // TODO recreate if apply is not possible

        if (minecraftServer.spec.persistence?.enabled == true) {
            val updatedStatefulSet =
                (
                    withContext(Dispatchers.IO) {
                        deployments
                            .inNamespace(minecraftServer.metadata.namespace)
                            .withName(minecraftServer.metadata.name)
                            .delete()
                        statefulSets
                            .inNamespace(minecraftServer.metadata.namespace)
                            .withName(minecraftServer.metadata.name)
                            .get()
                    } ?: StatefulSet()
                ).edit()
                    .minecraftService(minecraftServer)
                    .editMetadata()
                    .withManagedFields()
                    .endMetadata()
                    .build()
            withContext(Dispatchers.IO) {
                statefulSets.resource(updatedStatefulSet).serverSideApply()
            }
        } else {
            val updatedDeployment =
                (
                    withContext(Dispatchers.IO) {
                        statefulSets
                            .inNamespace(minecraftServer.metadata.namespace)
                            .withName(minecraftServer.metadata.name)
                            .delete()
                        deployments
                            .inNamespace(minecraftServer.metadata.namespace)
                            .withName(minecraftServer.metadata.name)
                            .get()
                    } ?: Deployment()
                ).edit()
                    .minecraftService(minecraftServer)
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
        private val logger = LoggerFactory.getLogger(MinecraftServersSubsystem::class.java)
    }
}
