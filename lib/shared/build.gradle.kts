plugins {
    idea
    alias(libs.plugins.kotlin.jvm)
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
ktlint {
    ignoreFailures.set(true)
}