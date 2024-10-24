plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.google.jib)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.kotlin.ktlint)
}

repositories {
    mavenCentral()
}
dependencies {
    implementation(project(":lib:shared"))
    implementation(project(":lib:crds"))
    implementation(project(":lib:k8s"))
    implementation(project(":lib:app-commons"))

    implementation(libs.bundles.kotlin.stdlib)
    implementation(libs.bundles.kotlin.coroutines)
    implementation(libs.bundles.logging)
    implementation(libs.bundles.bouncycastle)
    implementation(libs.bundles.koin)
    implementation(libs.k8s.client)

    implementation(libs.picocli)
}
kotlin {
    jvmToolchain(21)
}
application {
    mainClass.set("de.phyrone.nautilus.operator.OperatorApplication")
}
jib {
    from {
        image = "eclipse-temurin:21-jdk"
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }
    container {
    }
}
ktlint {
    ignoreFailures.set(true)
}
