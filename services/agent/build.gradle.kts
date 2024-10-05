plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

dependencies{
    implementation(project(":agent:paper"))
}

tasks{
    jar{
        archiveClassifier.set("no-dependencies")
    }
    shadowJar{
        enabled = true
        archiveClassifier.set("")
    }
    build{
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
