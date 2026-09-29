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
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    named("crd2java") {
        // The generator never removes classes of CRDs/fields that no longer exist
        doFirst { delete(crdClassedDir) }
    }
    compileJava {
        dependsOn("crd2java")
    }
}
javaGen {
    this.enumUppercase = true
    source.set(file("src/main/k8s"))
    this.packageOverrides = mapOf(
        "de.phyrone.nautilus.v1alpha1" to "de.phyrone.nautilus.k8s.crds.v1alpha1",
        "de.phyrone.nautilus.v1" to "de.phyrone.nautilus.k8s.crds.v1"
    )
    // Fields that embed upstream Kubernetes types use the fabric8 model classes instead of generated copies
    val v1alpha1 = "de.phyrone.nautilus.k8s.crds.v1alpha1"
    val objectReference = "io.fabric8.kubernetes.api.model.ObjectReference"
    this.existingJavaTypes = mapOf(
        "$v1alpha1.minecraftserverspec.PodOverrides" to "io.fabric8.kubernetes.api.model.PodTemplateSpec",
        "$v1alpha1.minecraftproxyspec.PodOverrides" to "io.fabric8.kubernetes.api.model.PodTemplateSpec",
        "$v1alpha1.minecraftserverspec.DeploymentStrategy" to "io.fabric8.kubernetes.api.model.apps.DeploymentStrategy",
        "$v1alpha1.minecraftserverspec.Cluster" to objectReference,
        "$v1alpha1.minecraftproxyspec.install.Cluster" to objectReference,
        "$v1alpha1.minecraftserverspec.template.Ref" to objectReference,
        "$v1alpha1.minecraftproxyspec.template.Ref" to objectReference,
        "$v1alpha1.minecraftclusterstatus.Servers" to objectReference,
        "$v1alpha1.minecraftclusterstatus.Proxies" to objectReference,
    )
    this.target.set(crdClassedDir)
}