rootProject.name = "customs-product-service"

include(
    "integration-test",
    "kotlin-client",
    "kotlin-client:api",
    "kotlin-client:client",
    "lib",
    "lib:auth",
    "lib:backgroundjob",
    "lib:config",
    "lib:distributedlock",
    "lib:graphql",
    "lib:grpc",
    "lib:jooq",
    "lib:jooq-testing",
    "lib:kafka-outbox",
    "lib:kinesis",
    "lib:protobuf-util",
    "lib:protobuf-util",
    "lib:s3",
    "lib:sqs",
    "lib:switches",
    "server",
    "stub",
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        // TODO EUC-5544: Move these versions to the libraries project
        create("libDependencies") {
            // kotlinx coroutines
            version("kotlinx-coroutines", "1.7.3")
            library(
                "kotlinx-coroutines-core",
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-core",
            ).versionRef("kotlinx-coroutines")

            // kotlinx serialization
            library(
                "kotlinx-serialization-json",
                "org.jetbrains.kotlinx",
                "kotlinx-serialization-json",
            ).version("1.6.0")

            // Testing
            library("junit", "org.junit.jupiter", "junit-jupiter").version("5.10.0")
            version("kotest", "5.6.2")
            library("kotest-assertions-core", "io.kotest", "kotest-assertions-core").versionRef("kotest")
            library("kotest-assertions-json", "io.kotest", "kotest-assertions-json").versionRef("kotest")
            library("mockk", "io.mockk", "mockk").version("1.13.8")
            library("kotlinx-coroutines-test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test")
                .versionRef("kotlinx-coroutines")
            bundle(
                "testing",
                listOf(
                    "junit",
                    "kotest-assertions-core",
                    "kotest-assertions-json",
                    "mockk",
                    "kotlinx-coroutines-test",
                ),
            )

            // UUID v7
            library("uuid", "com.github.f4b6a3", "uuid-creator").version("6.0.0")
        }

        // Shared libraries and bundles
        create("libs") {
            // Database
            version("jooq", "3.18.2")
            library("jooq-jooq", "org.jooq", "jooq").versionRef("jooq")
            library("jooq-kotlin", "org.jooq", "jooq-kotlin").versionRef("jooq")
            library("jooq-coroutines", "org.jooq", "jooq-kotlin-coroutines").versionRef("jooq")
            bundle("jooq", listOf("jooq-jooq", "jooq-kotlin", "jooq-coroutines"))
            library("hikaricp", "com.zaxxer", "HikariCP").version("5.0.1")

            // Hoplite
            version("hoplite", "2.7.5")
            library("hoplite-core", "com.sksamuel.hoplite", "hoplite-core").versionRef("hoplite")
            library("hoplite-yaml", "com.sksamuel.hoplite", "hoplite-yaml").versionRef("hoplite")
            library("hoplite-aws", "com.sksamuel.hoplite", "hoplite-aws").versionRef("hoplite")
            bundle("hoplite", listOf("hoplite-core", "hoplite-yaml", "hoplite-aws"))

            // AWS
            version("aws-java-sdk", "1.12.525")
            library("aws-java-sdk-sts", "com.amazonaws", "aws-java-sdk-sts").versionRef("aws-java-sdk")

            // gRPC + flexport RPC
            // The grpc version should match the version of io.grpc:grpc-protobuf as declared in
            // com.flexport.rpc:rpc. To find that version, see the output of
            // `gradle -q server:dependencies --configuration runtimeClasspath`
            version("grpc", "1.50.1")
            version("grpcKotlin", "1.3.1")
            library("grpc-flexportRpc", "com.flexport.rpc", "rpc").version("1.4.1")
            library("grpc-kotlinStub", "io.grpc", "grpc-kotlin-stub").versionRef("grpcKotlin")
            library("grpc-netty", "io.grpc", "grpc-netty").versionRef("grpc")
            library("grpc-nettyShaded", "io.grpc", "grpc-netty-shaded").versionRef("grpc")
            library("grpc-protobuf", "io.grpc", "grpc-protobuf").versionRef("grpc")
            library("grpc-stub", "io.grpc", "grpc-stub").versionRef("grpc")
            library("grpc-testing", "io.grpc", "grpc-testing").versionRef("grpc")
            // grpc-testing does not support Junit5 yet: https://github.com/grpc/grpc-java/issues/5331
            library("grpc-testing-junit5-cleanup", "com.asarkar.grpc", "grpc-test").version("1.2.2")
            bundle("grpc-testing", listOf("grpc-testing", "grpc-testing-junit5-cleanup"))

            // Protobuf
            version("protobuf", "3.21.7")
            library("protobuf-javaUtil", "com.google.protobuf", "protobuf-java-util").versionRef("protobuf")
            library("protobuf-kotlin", "com.google.protobuf", "protobuf-kotlin").versionRef("protobuf")
            bundle("protobuf", listOf("protobuf-javaUtil", "protobuf-kotlin"))

            // Graphql
            version("graphql", "6.2.2")
            library("graphql-kotlin-server", "com.expediagroup", "graphql-kotlin-server").versionRef("graphql")
            library("graphql-kotlin-hooks-provider", "com.expediagroup", "graphql-kotlin-hooks-provider")
                .versionRef("graphql")
            library(
                "graphql-kotlin-schema-generator",
                "com.expediagroup",
                "graphql-kotlin-schema-generator",
            ).versionRef("graphql")
            library("graphql-kotlin-sdl-generator", "com.expediagroup", "graphql-kotlin-sdl-generator")
                .versionRef("graphql")
            library("graphql-kotlin-federation", "com.expediagroup", "graphql-kotlin-federation").versionRef("graphql")
            bundle(
                "graphql",
                listOf(
                    "graphql-kotlin-server",
                    "graphql-kotlin-hooks-provider",
                    "graphql-kotlin-schema-generator",
                    "graphql-kotlin-sdl-generator",
                    "graphql-kotlin-federation",
                ),
            )
        }
    }
}
