plugins {
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.graphql)
}

val wiremockExtensions: Configuration by configurations.creating

dependencies {
    implementation(project(":stub"))
    implementation(libs.grpc.nettyShaded)

    // Cucumber requirements
    implementation("io.cucumber", "cucumber-java8", "7.14.0")
    implementation("io.cucumber", "cucumber-junit", "7.14.0")
    implementation("io.cucumber", "cucumber-junit-platform-engine", "7.14.0")
    implementation("org.junit.platform", "junit-platform-suite", "1.8.2")

    // HTTP client for integration tests
    val ktorVersion = "2.3.3"
    implementation("io.ktor", "ktor-client-cio", ktorVersion)

    // Wiremock support
    implementation("org.wiremock", "wiremock", "3.9.2")
    wiremockExtensions("org.wiremock", "wiremock-grpc-extension-standalone", "0.8.1")

    // Database support to clean database state
    implementation("org.postgresql", "postgresql", "42.6.0")

    // Assertions
    implementation("io.kotest", "kotest-assertions-core", "5.6.2")
}

val databaseConfiguration = project.extensions.getByType(DatabaseConfiguration::class)

tasks.register<Copy>("copyWiremockExtensions") {
    val wiremockExtensionJars: Set<ResolvedArtifact> = wiremockExtensions
        .resolvedConfiguration
        .firstLevelModuleDependencies
        .flatMap { it.moduleArtifacts }
        .toSet()

    from(wiremockExtensionJars.map { it.file })
    into("${layout.buildDirectory.get()}/extensions")
}

tasks.named("assemble") {
    dependsOn(":stub:generateProto")
}

tasks.named("build") {
    dependsOn("copyWiremockExtensions")
}

tasks.test {
    environment(
        mapOf(
            "DB_URL" to databaseConfiguration.url.get(),
            "DB_USERNAME" to databaseConfiguration.username.get(),
            "DB_PASSWORD" to databaseConfiguration.password.get(),
        ),
    )
}
