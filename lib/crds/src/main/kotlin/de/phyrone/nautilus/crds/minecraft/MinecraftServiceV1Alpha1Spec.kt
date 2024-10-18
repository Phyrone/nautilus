package de.phyrone.nautilus.crds.minecraft

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class MinecraftServiceV1Alpha1Spec(
    val baseImage: String? = null,
    val template: TemplateSpec? = null,
) {
    data class TemplateSpec(
        val include: List<String>? = null,
    ) {

    }

}