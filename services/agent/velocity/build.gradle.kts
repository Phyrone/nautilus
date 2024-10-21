plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.jetbrains.dokka)
}



repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies {
    implementation(project(":agent:shared"))

    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    // Coroutines
    implementation(libs.bundles.mcroutines.velocity)

}
kotlin {
    jvmToolchain(21)
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

}