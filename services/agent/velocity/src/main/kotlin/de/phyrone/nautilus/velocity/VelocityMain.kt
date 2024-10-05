package de.phyrone.nautilus.velocity

import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin

@Plugin(
    id = "nautilus-cloud",
    name = "Nautilus Cloud Agent",
)
class VelocityMain @Inject constructor(suspendingPluginContainer: SuspendingPluginContainer) {

    init {
        suspendingPluginContainer.initialize(this)
    }

    @Subscribe
    suspend fun ProxyInitializeEvent.proxyInit() {


    }

}