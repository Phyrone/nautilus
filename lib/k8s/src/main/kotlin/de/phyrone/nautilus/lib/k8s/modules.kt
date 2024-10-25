package de.phyrone.nautilus.lib.k8s

import io.fabric8.kubernetes.client.KubernetesClientBuilder
import org.koin.dsl.module

val k8sModule =
    module {
        single { KubernetesClientBuilder().build() }
    }
