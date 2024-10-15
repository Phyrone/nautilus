plugins {
    application
    kotlin("jvm")
    kotlin("kapt")

    id("com.google.cloud.tools.jib")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io"){

    }
}

dependencies {
    implementation(project(":sub:mc-image-helper"))

    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("info.picocli:picocli:4.7.6")
    kapt("info.picocli:picocli-codegen:4.7.6")

    implementation("com.google.cloud.tools:jib-core:0.27.1")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.0.0.202409031743-r")

    implementation("me.tongfei:progressbar:0.10.1")


}