package de.phyrone.nautilus.lib.k8s

import de.phyrone.nautilus.shared.runWhen
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderCallbacks
import io.fabric8.kubernetes.client.extended.leaderelection.resourcelock.LeaseLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.future.await
import org.koin.core.component.KoinComponent

class LeaderElector(
    private val kubernetesClient: KubernetesClient,
    private val namespace: String,
    private val name: String,
    private val identity: String,
) : KoinComponent {
    private val currentLeaderMut = MutableStateFlow("")
    private val isLeaderMut = MutableStateFlow(false)
    val currentLeader = currentLeaderMut.asStateFlow()

    val isLeader = isLeaderMut.asStateFlow()
    private val lock = LeaseLock(namespace, name, identity)
    private val callbacks = LeaderCallbacks(::onGain, ::onResign, ::onChange)
    private val leaderElectionConfig =
        io.fabric8.kubernetes.client.extended.leaderelection.LeaderElectionConfig(
            lock,
            // Lease duration
            java.time.Duration.ofSeconds(30),
            // Renew deadline
            java.time.Duration.ofSeconds(20),
            // Retry period
            java.time.Duration.ofSeconds(5),
            callbacks,
            true,
            identity,
        )

    fun onGain() {
        isLeaderMut.value = true
    }

    fun onResign() {
        isLeaderMut.value = false
    }

    fun onChange(leader: String) {
        currentLeaderMut.value = leader
    }

    suspend fun run() {
        kubernetesClient.leaderElector()
            .withConfig(leaderElectionConfig).build()
            .start()
            .await()
    }

    /**
     * Execute the following block when the current instance is the leader.
     * It will suspend until the instance is the leader.
     * The function will cancel when the instance is not the leader anymore but the block is not finished.
     */
    suspend fun <T> withLeadership(block: suspend () -> T): T = runWhen<T>(isLeader, block)
}
