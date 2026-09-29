plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.dokka)
}



repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies {
    implementation(project(":agent:shared"))

    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // Coroutines
    implementation(libs.bundles.mcroutines.folia)

}
kotlin {
    jvmToolchain(21)
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

}