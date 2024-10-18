plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.google.jib)
    alias(libs.plugins.kotlin.ktlint)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib:shared"))

    implementation(libs.bundles.logging)
    implementation(libs.bundles.kotlin.stdlib)
    implementation(libs.bundles.kotlin.coroutines)

    implementation("info.picocli:picocli:4.7.6")
    kapt("info.picocli:picocli-codegen:4.7.6")

    implementation(libs.bundles.jackson)

    implementation(libs.guava)

    implementation(libs.jib)
    implementation(libs.jgit)

    implementation("me.tongfei:progressbar:0.10.1")

    implementation(libs.k8s.client)

    implementation("com.sksamuel.aedile:aedile-core:1.3.1")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}

kotlin {
    jvmToolchain(21)
}
