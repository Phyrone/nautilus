plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.dokka)
}
repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation(libs.bundles.mcroutines.bungee)
    compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
}

kotlin {
    jvmToolchain(21)
}