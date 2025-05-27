import com.google.protobuf.gradle.proto

plugins {
    idea
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.google.protobuf)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.kotlin.stdlib)
    implementation(libs.bundles.kotlin.coroutines)

    implementation(libs.bundles.grpc)


}

sourceSets {
    main {
        proto {
            srcDir(rootDir.resolve("proto"))
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.31.0"
        //artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.version}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.73.0"
            //artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.version}"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
            //artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpc.kotlin.version}:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}