plugins {
    alias(libs.plugins.kotlin.jvm)
}
repositories {
    mavenCentral()
}
dependencies{
    implementation(libs.bundles.kotlin.stdlib)
    implementation(libs.bundles.kotlin.coroutines)
    implementation(libs.bundles.koin)

    implementation(libs.bundles.ktor.client)
    implementation("io.ktor:ktor-client-okhttp-jvm:3.1.2")


}