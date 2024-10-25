plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.dokka)
}
repositories{
    mavenCentral()
}

dependencies{
    implementation(libs.bundles.kotlin.stdlib)
    implementation(libs.bundles.jackson)
}