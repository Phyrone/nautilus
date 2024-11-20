package de.phyrone.nautilus.lib.k8s

import io.fabric8.kubernetes.api.model.DeletionPropagation
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.dsl.AnyNamespaceOperation
import io.fabric8.kubernetes.client.dsl.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


suspend inline fun <reified T, reified L, reified R : Resource<T>> AnyNamespaceOperation<T, L, R>.reconcile(
    item: T
) {

    withContext(Dispatchers.IO) {
        try {
            resource(item).forceConflicts().serverSideApply()
        } catch (e: KubernetesClientException) {
            println("Error during apply: ${e.message} attempting to recreate resource")
            //On conflict try to recreate the resource
            while (resource(item).get() != null) {
                resource(item).withPropagationPolicy(DeletionPropagation.FOREGROUND).delete()
                delay(1000)
            }
            resource(item).forceConflicts().serverSideApply()
        }
    }
}