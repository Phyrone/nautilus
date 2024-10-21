package de.phyrone.nautilus.builder.shared

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URL

//TODO
data class Cookbook(
    val baseImage: String? = null,
) {


}