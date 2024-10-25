plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.kotlin.ktlint)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib:shared"))
    implementation(project(":lib:crds"))

    implementation(libs.bundles.kotlin.stdlib)
    implementation(libs.bundles.kotlin.coroutines)
    implementation(libs.k8s.client)
    implementation(libs.bundles.koin)
    implementation(libs.guava)
}

ktlint {
    ignoreFailures.set(true)
}
