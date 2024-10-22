package de.phyrone.nautilus.lib.k8s.utils

import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.Watcher.Action
import io.fabric8.kubernetes.client.WatcherException
import io.fabric8.kubernetes.client.dsl.Watchable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow

class WatchEventFlow<T>(
    private val watchable: Watchable<T>,
) : Flow<Pair<Action, T>> {
    class SocketClosedException(cause: Throwable?) : RuntimeException(cause)

    private sealed interface Event

    private data class WatchEvent<T>(val action: Action, val item: T) : Event

    private data class CloseEvent(val exception: WatcherException?) : Event

    override suspend fun collect(collector: FlowCollector<Pair<Action, T>>) {
        val events = MutableSharedFlow<Event>(extraBufferCapacity = Int.MAX_VALUE)
        watchable.watch(
            object : Watcher<T> {
                override fun eventReceived(
                    action: Action,
                    resource: T,
                ) {
                    events.tryEmit(WatchEvent(action, resource))
                }

                override fun onClose(cause: WatcherException?) {
                    events.tryEmit(CloseEvent(cause))
                }
            },
        ).use {
            events.collect {
                @Suppress("UNCHECKED_CAST")
                when (it) {
                    is WatchEvent<*> -> collector.emit(it.action to it.item as T)
                    is CloseEvent -> throw SocketClosedException(it.exception)
                }
            }
        }
    }
}
