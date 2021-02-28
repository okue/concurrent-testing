import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    `java-library`
    kotlin("jvm")
    id("com.google.protobuf") version Versions.PROTOBUF_PLUGIN
}

dependencies {
    // grpc
    testImplementation("com.google.protobuf:protobuf-java:${Versions.PROTOBUF}")
    testImplementation("io.grpc:grpc-protobuf:${Versions.GRPC}")
    testImplementation("io.grpc:grpc-stub:${Versions.GRPC}")
    testImplementation("io.grpc:grpc-kotlin-stub:${Versions.GRPC_KT}")
    testImplementation("javax.annotation:javax.annotation-api:1.3.2")
}

protobuf {
    generatedFilesBaseDir = "$projectDir/gen-src"
    protoc {
        artifact = "com.google.protobuf:protoc:${Versions.PROTOBUF}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${Versions.GRPC}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${Versions.GRPC_KT}:jdk7@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}
