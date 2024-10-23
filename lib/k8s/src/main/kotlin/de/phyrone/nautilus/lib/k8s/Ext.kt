package de.phyrone.nautilus.lib.k8s

import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.ObjectReference
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder

fun HasMetadata.toObjectReference(): ObjectReference =
    ObjectReferenceBuilder()
        .withName(this.metadata.name)
        .withNamespace(this.metadata.namespace)
        .withUid(this.metadata.namespace)
        .withKind(this.kind)
        .withApiVersion(this.apiVersion)
        .build()
