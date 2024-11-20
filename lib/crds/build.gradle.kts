plugins {
    java
    alias(libs.plugins.fabric8.crds.generator)
}


val crdClassedDir = layout.buildDirectory.dir("generated/crd")
sourceSets {
    main {
        java {
            srcDir(crdClassedDir)
        }
    }
}
repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.k8s.client)
    implementation(libs.k8s.crds.annotations)
    annotationProcessor(libs.k8s.crds.generator)

    implementation(libs.builder.annotations)
    annotationProcessor(libs.builder.annotations)

    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

}

tasks {
    compileJava {
        dependsOn("crd2java")
    }
}
javaGen {
    this.enumUppercase = true
    source.set(file("src/main/k8s"))
    this.packageOverrides = mapOf(
        "de.phyrone.nautilus.v1alpha1" to "de.phyrone.nautilus.k8s.crds.v1alpha1",
        "de.phyrone.nautilus.v1" to "de.phyrone.nautilus.k8s.crds.v1",
        "dev.agones.v1" to "de.phyrone.nautilus.k8s.crds.dev.agones.v1",
        "dev.agones.autoscaling.v1" to "de.phyrone.nautilus.k8s.crds.dev.agones.autoscaling.v1",
    )
    this.generatedAnnotations = true
    this.extraAnnotations = true
    this.existingJavaTypes = mapOf(
        //"" to ""
    )

    this.target.set(crdClassedDir)

}