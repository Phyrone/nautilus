package de.phyrone.nautilus.appcommons

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.internal.MainDispatcherFactory

@OptIn(InternalCoroutinesApi::class)
class MainLoopFactory : MainDispatcherFactory {
    override val loadPriority: Int = UShort.MAX_VALUE.toInt()

    override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher {
        return MainLoop
    }
}