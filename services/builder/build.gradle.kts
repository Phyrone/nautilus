plugins {

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.google.jib)
    alias(libs.plugins.kotlin.ktlint)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib:shared"))
    implementation(project(":builder:shared"))

    implementation(libs.bundles.logging)
    implementation(libs.bundles.kotlin.stdlib)
    implementation(libs.bundles.kotlin.coroutines)
    implementation(libs.picocli)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.koin)
    implementation(libs.guava)
    implementation(libs.jgit)
    // implementation("me.tongfei:progressbar:0.10.1")
    implementation(libs.k8s.client)
    implementation(libs.jib)
}

jib {
    from {
    }
    container {
    }
}
