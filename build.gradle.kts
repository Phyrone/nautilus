plugins {
    idea
    base
    kotlin("jvm") version "2.0.20-RC" apply false
    id("com.github.ben-manes.versions") version "0.51.0"
    //id("kr.entree.spigradle") version "2.4.3" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
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