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
}

dependencies {
    dokkaPlugin("org.jetbrains.dokka:mathjax-plugin:1.9.20")
    dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.9.20")
}

tasks {
    named("dokkaHtmlMultiModule") {
        dependsOn(project.getTasksByName("dokkaHtmlMultiModule", true).also { it.remove(this) })
    }
    named("dokkaJekyllMultiModule") {
        dependsOn(project.getTasksByName("dokkaJekyllMultiModule", true).also { it.remove(this) })
    }
    named("dokkaGfmMultiModule") {
        dependsOn(project.getTasksByName("dokkaGfmMultiModule", true).also { it.remove(this) })
    }

    clean {
        this.setDelete(projectDir.resolve("build"))
        this.setDelete(projectDir.resolve("target"))
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
