plugins {
    kotlin("jvm")
}



repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.18.1-R0.1-SNAPSHOT")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-folia-api:2.20.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-folia-core:2.20.0")

}
kotlin {
    jvmToolchain(21)
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

}