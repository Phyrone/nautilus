plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":agent:paper"))
    implementation(project(":agent:velocity"))
    implementation(project(":agent:bungee"))
}

tasks {
    jar {
        archiveClassifier.set("no-dependencies")
    }
    shadowJar {
        enabled = true
        archiveClassifier.set("")

        // Let transformers see every duplicate (service files, kotlin module metadata), drop other duplicates
        // https://gradleup.com/shadow/configuration/merging/#handling-duplicates-strategy
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
        filesNotMatching(listOf("META-INF/services/**", "META-INF/*.kotlin_module")) {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
    build {
        dependsOn(shadowJar)
    }
}

kotlin {
    jvmToolchain(21)
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
