plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.google.jib)
}
repositories{
    mavenCentral()
}
dependencies{
    implementation(libs.bundles.kotlin.stdlib)
    implementation(libs.bundles.kotlin.coroutines)
}