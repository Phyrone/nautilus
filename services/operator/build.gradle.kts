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
    implementation(libs.bundles.kotlin.stdlib)
    implementation(libs.bundles.kotlin.coroutines)
}
kotlin {
    jvmToolchain(21)
}
