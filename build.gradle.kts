plugins {
    idea
    base

    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.plugin.noarg) apply false
    alias(libs.plugins.kotlin.plugin.spring) apply false
    alias(libs.plugins.google.jib) apply false

    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.versions)
    alias(libs.plugins.kotlin.ktlint)
}

version = "0.0.1-SNAPSHOT"
group = "de.phyrone"

allprojects {
    this.project.version = rootProject.version
    this.project.group = rootProject.group
    repositories {
        mavenCentral()
    }
    // Compile every JVM module with Java 21, independent of the JDK running Gradle
    // (the Minecraft platform APIs in use target Java 21)
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}

dependencies {
    val dokkaVersion =
        libs.versions.dokka.version
            .get()
    dokkaPlugin("org.jetbrains.dokka:mathjax-plugin:$dokkaVersion")
    dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:$dokkaVersion")

    // Modules aggregated into the multi-module documentation (./gradlew dokkaGenerate -> build/dokka/html)
    dokka(project(":lib:grpc"))
    dokka(project(":lib:k8s"))
    dokka(project(":lib:shared"))
    dokka(project(":agent"))
    dokka(project(":agent:shared"))
    dokka(project(":agent:paper"))
    dokka(project(":agent:velocity"))
    dokka(project(":agent:bungee"))
    dokka(project(":builder"))
    dokka(project(":builder:shared"))
    dokka(project(":provisioner"))
}

dokka {
    moduleName.set("Nautilus")
}

tasks {
    clean {
        // Also remove the Cargo target directory of the Rust workspace
        delete(layout.projectDirectory.dir("target"))
    }
}
idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
ktlint {
    ignoreFailures.set(true)
}
