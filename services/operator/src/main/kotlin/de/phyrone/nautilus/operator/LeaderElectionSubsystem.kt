package de.phyrone.nautilus.operator

import de.phyrone.nautilus.appcommons.Subsystem
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderCallbacks
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderElectionConfig
import io.fabric8.kubernetes.client.extended.leaderelection.resourcelock.LeaseLock
import io.fabric8.kubernetes.client.extended.leaderelection.resourcelock.Lock
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.future.await
import org.koin.core.component.inject
import org.koin.dsl.bind
import org.koin.dsl.module
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class LeaderElectionSubsystem : Subsystem {



    override suspend fun runSubsystem() {


    }

    companion object {
        private val logger = LoggerFactory.getLogger(LeaderElectionSubsystem::class.java)
    }
}