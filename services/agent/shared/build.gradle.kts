plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.bundles.kotlin.stdlib)
    implementation(kotlin("reflect"))
    implementation(project(":lib:grpc"))

}

kotlin {
    jvmToolchain(21)
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

}
