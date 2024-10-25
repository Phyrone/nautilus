package de.phyrone.nautilus.lib.k8s

import com.google.common.io.Resources

object CrdUtils {
    val crdFilesUrl = Resources.getResource(this::class.java, "/META-INF/fabric8/")

    init {
        crdFilesUrl
    }
}

fun main(args: Array<String>) {
    val crdFilesUrl = Resources.getResource(CrdUtils::class.java, "/META-INF/fabric8/")
    println(crdFilesUrl)
}
