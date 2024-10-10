plugins {

    application
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.dokka")
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(project(":lib:grpc"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.0.0.202409031743-r")

    kapt("info.picocli:picocli-codegen:4.7.6")
    implementation("info.picocli:picocli:4.7.6")



}

application {
    mainClass.set("de.phyrone.nautilus.provisioner.ProvisionerMain")
}

jib {
    from {
        image = "azul/zulu-openjdk-alpine:21-jre"
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
        this.labels.putAll(mapOf(
            "org.opencontainers.image.source" to "https://github.com/Phyrone/nautilus",
            "org.opencontainers.image.licenses" to "EUPL-1.2",

        ))
    }
}