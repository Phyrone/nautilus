package de.phyrone.nautilus.lib.k8s

import de.phyrone.nautilus.lib.k8s.utils.WatchEventFlow
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.Watchable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ResourceDeletedException(
    val resource: HasMetadata,
) : CancellationException()

/**
 * Run a job for each resource.
 * Each resource added will result in a new job being started.
 * This job contains the resource as a state flow.
 * A job that completes before the resource is deleted will restart when the resource is modified.
 * The job will be canceled when the resource is deleted.
 */
suspend fun <T : HasMetadata> runResourceWatchers(
    watchable: Watchable<T>,
    block: suspend ResourceJobScope<T>.(StateFlow<T>) -> Unit,
) {
    class ResourceJobScopeImpl(
        initial: T,
    ) : ResourceJobScope<T> {
        override val name: String = initial.metadata.name
        override val namespace: String = initial.metadata.namespace

        val stateFlowMut = MutableStateFlow(initial)
        val resource: StateFlow<T> = stateFlowMut.asStateFlow()
    }

    class ResourceJob<T : HasMetadata>(
        val job: Job,
        val scope: ResourceJobScopeImpl,
    )
    coroutineScope {
        val jobsLock = Mutex()
        val jobs = mutableMapOf<String, ResourceJob<T>>()
        try {
            WatchEventFlow(watchable).collect { (action, resource) ->
                when (action) {
                    Watcher.Action.ADDED, Watcher.Action.MODIFIED -> {
                        jobsLock.withLock {
                            val activeJob = jobs[resource.metadata.uid]
                            if (activeJob == null) {
                                val scope = ResourceJobScopeImpl(resource)
                                val job =
                                    launch {
                                        try {
                                            block(scope, scope.resource)
                                        } finally {
                                            jobsLock.withLock { jobs.remove(resource.metadata.uid) }
                                        }
                                    }
                                jobs[resource.metadata.uid] = ResourceJob(job, scope)
                            } else {
                                activeJob.scope.stateFlowMut.value = resource
                            }
                        }
                    }

                    Watcher.Action.DELETED -> {
                        jobsLock.withLock {
                            jobs.remove(resource.metadata.uid)?.job
                        }?.cancel(ResourceDeletedException(resource))
                    }

                    Watcher.Action.ERROR -> println("Error on resource ${resource.metadata.name} $resource")
                    Watcher.Action.BOOKMARK -> {}
                }
            }
        } finally {
            jobs.values.forEach { it.job.cancel() }
        }
    }
}
