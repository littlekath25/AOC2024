import com.google.protobuf.gradle.id

plugins {
    id("com.google.protobuf")
    idea
}

kotlin {
    compilerOptions {
        // Generated protobuf code often contains deprecated code, e.g. due to deprecated fields
        // this creates a lot of warning noise which is not useful as it works as intended and we
        // do not control the deprecated code.
        freeCompilerArgs.add("-Xsuppress-warning=DEPRECATION")
    }
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    api(project(":lib:grpc"))

    api(libs.grpc.kotlinStub)
    api(libs.grpc.protobuf)
    api(libs.grpc.stub)
    api(libs.bundles.protobuf)

    // Monorepo published artifacts with .proto files
    // protobuf("com.flexport.my.package", "my-event-kt", "1.0.0-beta.1.0")
}

// Improve proto handling in IntelliJ
idea {
    module {
        generatedSourceDirs.addAll(
            files(
                "build/generated/source/proto/main/java",
                "build/generated/source/proto/main/kotlin",
                "build/generated/source/proto/main/grpc",
                "build/generated/source/proto/main/grpckt",
                "build/extracted-include-protos/main",
                "build/extracted-protos/main",
            ),
        )
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

val descriptorPath = "$projectDir/build/descriptors"
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpcKotlin.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            // Protobuf descriptors are used for wiremock stubs in functional tests,
            // but not for the app itself.
            task.generateDescriptorSet = true
            task.descriptorSetOptions.includeImports = true
            task.descriptorSetOptions.path =
                "$descriptorPath/${task.sourceSet.name}.dsc"

            task.plugins {
                id("grpc")
                id("grpckt")
            }

            task.builtins {
                id("kotlin")
            }
        }
    }
}

tasks.named("generateProto") {
    finalizedBy("createDescriptorsDir")
}

// In case there a no proto files, the directory for descriptors is not created,
// but we want the wiremock Docker container to be able to mount it in any case.
tasks.register("createDescriptorsDir") {
    file(descriptorPath).apply {
        if (!exists()) {
            mkdirs()
        }
    }
}
