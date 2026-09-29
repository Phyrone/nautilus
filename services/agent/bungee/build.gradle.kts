plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.dokka)
}
repositories {
    mavenCentral()
    // bungeecord-protocol depends on brigadier, which is only published by Mojang
    exclusiveContent {
        forRepository { maven("https://libraries.minecraft.net") }
        filter { includeGroup("com.mojang") }
    }
}

dependencies {
    implementation(libs.bundles.mcroutines.bungee)
    compileOnly("net.md-5:bungeecord-api:1.21-R0.4")
}

kotlin {
    jvmToolchain(21)
}