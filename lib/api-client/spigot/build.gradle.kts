plugins {
    alias(libs.plugins.kotlin.jvm)
}
repositories {
    mavenCentral()
}
dependencies{
    implementation(project(":lib:api-client:common"))

    implementation(libs.bundles.kotlin.stdlib)
    implementation(libs.bundles.kotlin.coroutines)

    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.koin)

}