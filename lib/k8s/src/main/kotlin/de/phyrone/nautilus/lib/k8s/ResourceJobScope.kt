package de.phyrone.nautilus.lib.k8s

import io.fabric8.kubernetes.api.model.HasMetadata
import kotlinx.coroutines.flow.StateFlow

interface ResourceJobScope<T : HasMetadata> {
    val name: String
    val namespace: String

    val resource: StateFlow<T>
}
