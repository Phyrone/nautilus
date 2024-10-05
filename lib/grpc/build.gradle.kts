import com.google.protobuf.gradle.proto

plugins {
    idea
    kotlin("jvm")
    id("com.google.protobuf")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.grpc:grpc-netty-shaded:1.68.0")
    implementation("io.grpc:grpc-protobuf:1.68.0")
    implementation("io.grpc:grpc-stub:1.68.0")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")

    implementation("com.google.protobuf:protobuf-kotlin:4.28.2")
    implementation("com.google.protobuf:protobuf-java-util:4.28.2")



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
        artifact = "com.google.protobuf:protoc:4.28.2"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.68.0"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
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