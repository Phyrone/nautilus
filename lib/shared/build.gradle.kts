plugins {
    idea
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.dokka)
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