plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":agent:paper"))
    implementation(project(":agent:velocity"))
    implementation(project(":agent:bungee"))
}

tasks {
    jar {
        archiveClassifier.set("no-dependencies")
    }
    shadowJar {
        enabled = true
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
}

kotlin {
    jvmToolchain(21)
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
