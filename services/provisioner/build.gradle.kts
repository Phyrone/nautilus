plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.google.jib)
    alias(libs.plugins.kotlin.ktlint)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib:shared"))

    implementation(libs.bundles.logging)
    implementation(libs.bundles.kotlin.stdlib)
    implementation(libs.bundles.kotlin.coroutines)
    implementation(libs.picocli)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.koin)
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("de.phyrone.nautilus.provisioner.ProvisionerMain")
}
jib {
    from {
        image = "eclipse-temurin:22-jdk"
        platforms {
            platform {
                os = "linux"
                architecture = "amd64"
            }
            platform {
                os = "linux"
                architecture = "arm64"
            }
        }
    }

    this.container {
        this.appRoot = "/app"
        this.workingDirectory = "/data"
    }
}
