plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.plugin.noarg)
    alias(libs.plugins.jetbrains.dokka)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib:shared"))

    implementation(libs.k8s.client)
    implementation(libs.k8s.crds.annotations)
    kapt(libs.k8s.crds.generator)
}

kapt {
    arguments {
        //arg("io.fabric8.crd.generator.parallel", "true")
    }
}
noArg {
    annotation("de.phyrone.nautilus.crds.NoArgs")
}