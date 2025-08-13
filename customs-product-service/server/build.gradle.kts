import com.expediagroup.graphql.plugin.gradle.graphql
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateSDLTask
import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Logging

plugins {
    id("application")
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.graphql)
    alias(libs.plugins.jooq)
    alias(libs.plugins.flyway)
    alias(libs.plugins.sentry)
}

application {
    mainClass.set("com.flexport.customsproductservice.ApplicationKt")
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "com.flexport.customsproductservice.ApplicationKt"
        }
    }

    register<Test>("junitIntegrationTest") {
        useJUnitPlatform {
            includeTags("integration")
        }

        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        mustRunAfter(":server:test")
    }

    named("check") {
        dependsOn("junitIntegrationTest")
    }
}

dependencies {
    implementation(project(":stub"))
    implementation(project(":lib:auth"))
    implementation(libs.customs.core)
    implementation(project(":lib:jooq"))
    implementation(libs.customs.logging)
    implementation(project(":lib:backgroundjob"))
    implementation(libs.customs.fid)
    testImplementation(project(":lib:jooq-testing"))
    implementation(libs.customs.metrics)
    implementation(libs.customs.tracing)

    // Http Server
    val ktorVersion = "2.2.2"
    implementation("io.ktor", "ktor-server-auth", ktorVersion)
    implementation("io.ktor", "ktor-server-core", ktorVersion)
    implementation("io.ktor", "ktor-server-netty", ktorVersion)
    implementation("io.ktor", "ktor-server-content-negotiation", ktorVersion)
    implementation("io.ktor", "ktor-serialization-jackson", ktorVersion)
    implementation("io.ktor", "ktor-server-double-receive", ktorVersion)
    // Note: Ktor kotlinx.serialization is deprecated in customs NGIF template will be removed.
    implementation("io.ktor", "ktor-serialization-kotlinx-json", ktorVersion)
    testImplementation("io.ktor", "ktor-server-test-host", ktorVersion)
    testImplementation("io.kotest.extensions", "kotest-assertions-ktor", "2.0.0")

    // Http Client
    implementation("io.ktor", "ktor-client-core", ktorVersion)
    implementation("io.ktor", "ktor-client-cio", ktorVersion)
    implementation("io.ktor", "ktor-client-content-negotiation", ktorVersion)
    implementation("io.ktor", "ktor-client-auth", ktorVersion)

    // Configuration
    implementation(project(":lib:config"))

    // Kafka
    implementation(libs.customs.kafka)
    implementation(project(":lib:kafka-outbox"))
    testImplementation(libs.customs.kafka.testing)

    // Kotlin
    implementation("org.jetbrains.kotlin", "kotlin-reflect")

    // Database deps
    val postgresVersion = "42.6.0"
    implementation(libs.bundles.jooq)
    implementation("org.postgresql", "postgresql", postgresVersion)
    implementation("com.zaxxer", "HikariCP", "5.0.1")
    implementation("org.flywaydb", "flyway-core", "9.0.1")

    // Jooq
    jooqGenerator(libs.bundles.jooq)
    jooqGenerator("org.postgresql", "postgresql", postgresVersion)
    jooqGenerator("jakarta.xml.bind", "jakarta.xml.bind-api", "3.0.1")
    jooqGenerator("org.glassfish.jaxb", "jaxb-runtime", "3.0.1")

    // gRPC Configuration
    implementation(libs.grpc.nettyShaded)
    implementation(libs.grpc.flexportRpc)
    testImplementation(libs.bundles.grpc.testing)
    compileOnly("org.apache.tomcat", "annotations-api", "6.0.53")

    // Graphql
    implementation(project(":lib:graphql"))
    implementation(libs.bundles.graphql)

    // Protobuf Util
    implementation(project(":lib:protobuf-util"))

    // Flexport-wide deps excluding gRPC
    implementation(libs.flexport.auth)
    implementation(libs.flexport.servicelookup)

    // Testing
    testImplementation(libs.customs.core.testing)
}

val databaseConfiguration = project.extensions.getByType(DatabaseConfiguration::class)

flyway {
    url = databaseConfiguration.url.get()
    user = databaseConfiguration.username.get()
    password = databaseConfiguration.password.get()
    cleanDisabled = false
}

buildscript {
    // Use our Jooq version in the gradle script.
    // This is because isKotlinNotNullPojoAttributes is jooq 3.18+, and nu.studer.jooq plugin is on 3.17
    // This can be removed once nu.studer.jooq releases a new version
    dependencies {
        "classpath"(group = "org.jooq", name = "jooq-meta", version = libs.versions.jooq.get())
    }
}
jooq {
    version.set(libs.versions.jooq.get())
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(true)
            jooqConfiguration.apply {
                logging = Logging.WARN
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = databaseConfiguration.url.get()
                    user = databaseConfiguration.username.get()
                    password = databaseConfiguration.password.get()
                }
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"

                        forcedTypes.add(
                            ForcedType().apply {
                                userType = "java.time.Instant"
                                converter = "com.flexport.customs.lib.jooq.converters.InstantConverter"
                                includeTypes = "TIMESTAMP"
                            },
                        )
                    }
                    generate.apply {
                        isDaos = true
                        isImmutableInterfaces = true
                        isImmutablePojos = true
                        isImplicitJoinPathsAsKotlinProperties = true
                        isInterfaces = true
                        isKotlinNotNullPojoAttributes = true
                        isPojosAsKotlinDataClasses = true
                        isRecords = true
                    }
                    target.apply {
                        packageName = "com.flexport.customsproductservice.db"
                        directory = "build/generated-src/jooq/main"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}

tasks.register("runDbMigration") {
    group = "application"
    description = "Runs the application in Database Migration mode with the local profile."
    doFirst {
        tasks.run.configure {
            args("--run-db-migrations")
        }
    }
    finalizedBy("run")
}

graphql {
    schema {
        packages = listOf("com.flexport.customsproductservice.graphql")
    }
}

// Sentry plugin: https://docs.sentry.io/platforms/java/gradle/
sentry {
    autoInstallation {
        enabled.set(true)
    }

    org.set("flexport")
    projectName.set("customs-product-service")
    authToken.set(System.getenv("SENTRY_AUTH_TOKEN"))
    includeSourceContext.set(!(System.getenv("SENTRY_AUTH_TOKEN")?.isNullOrEmpty() ?: true))
}

tasks.named("sentryCollectSourcesJava") {
    // We need to run all code generators before allowing sentry to collect sources
    mustRunAfter(":server:generateJooq")
}

tasks.named("build") { finalizedBy("graphqlGenerateSDL") }

val graphqlGenerateSDL by tasks.getting(GraphQLGenerateSDLTask::class) {
    schemaFile.set(File("${project.projectDir}/schema.graphqls"))
}
