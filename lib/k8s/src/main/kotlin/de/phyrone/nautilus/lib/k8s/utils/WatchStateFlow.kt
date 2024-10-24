package de.phyrone.nautilus.lib.k8s.utils

import de.phyrone.nautilus.shared.unreachable
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.ListOptions
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.WatcherException
import io.fabric8.kubernetes.client.dsl.Watchable
import kotlinx.coroutines.flow.*
import java.io.Closeable

class WatchStateFlow<T : HasMetadata>(
    watchable: Watchable<T>,
) : Closeable, Flow<Collection<T>>, StateFlow<Collection<T>> {
    private val stateMut = MutableStateFlow(emptyMap<String, T>())
    val state = stateMut.asStateFlow()

    override val replayCache: List<Collection<T>>
        get() = stateMut.replayCache.map { it.values }
    override val value: Collection<T>
        get() = stateMut.value.values
    private val watcher =
        object : Watcher<T> {
            override fun eventReceived(
                action: Watcher.Action,
                resource: T,
            ) {
                when (action) {
                    Watcher.Action.ADDED -> {
                        stateMut.update { it + (resource.metadata.uid to resource) }
                    }

                    Watcher.Action.MODIFIED -> {
                        stateMut.update { it + (resource.metadata.uid to resource) }
                    }

                    Watcher.Action.DELETED -> {
                        stateMut.update { it - resource.metadata.uid }
                    }

                    Watcher.Action.ERROR -> {}

                    Watcher.Action.BOOKMARK -> {}
                }
            }

            override fun onClose(cause: WatcherException?) {}
        }
    private val watch = watchable.watch(options, watcher)

    override suspend fun collect(collector: FlowCollector<Collection<T>>): Nothing {
        stateMut
            .map { it.values }
            .collect(collector)
        unreachable()
    }

    override fun close() {
        watch.close()
    }

    companion object {
        private val options =
            ListOptions().also { options ->
                options.watch = true
                options.allowWatchBookmarks = false
            }
    }
}
