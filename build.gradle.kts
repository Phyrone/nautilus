plugins {
    idea
    base
    kotlin("jvm") version "2.0.21" apply false
    kotlin("kapt") version "2.0.21" apply false

    id("org.jetbrains.dokka") version "1.9.20"
    id("com.github.ben-manes.versions") version "0.51.0"
    //id("kr.entree.spigradle") version "2.4.3" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false

    id("com.google.protobuf") version "0.9.4" apply false
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