plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.ktlint.gradle)
}

/** Inspects the quasi-standard "CI" environmental variable to see whether we are on CI **/
val onCI: Boolean = System.getenv("CI") == "true"

val artifactoryUrl = "https://artifactory.flexport.io:443/artifactory"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven {
            // Main Flexport maven repo
            url = uri("$artifactoryUrl/mvn/")
        }
        maven {
            // For PRs in the template repository, libraries are published here as version "x.y.z-git_sha"
            url = uri("$artifactoryUrl/customs-kotlin-service-full-libs-maven-stage-local/")
        }
        maven {
            // For PRs in customs-libs repository, libraries are published here as version "x.y.z-git_sha"
            url = uri("$artifactoryUrl/customs-domain-libs-maven-stage-local/")
        }
    }

    configurations.all {
        resolutionStrategy {
            // To make builds reproducible, disallow dynamic versions
            failOnDynamicVersions()
            // To make builds reproducible, disallow changing dependencies like SNAPSHOT on CI
            if (onCI) failOnChangingVersions()
            // Do not upgrade dependency versions automatically because of transitive dependencies
            // having higher versions of the same dependency.
            // See the build.gradle.kts files of this project and the gradle docs for how to resolve conflicts:
            // https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.ResolutionStrategy.html
            failOnVersionConflict()
            preferProjectModules()
        }
    }
}

val Project.isGeneratedOrTemplateManaged: Boolean
    get() = name == "stub" || name == "lib" || parent?.name == "lib"

// Apply JVM-specific configuration to subprojects
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply<DatabaseConfigurationProviderPlugin>()

    kotlin {
        // This also sets Java toolchain version
        jvmToolchain {
            languageVersion = JavaLanguageVersion.of(21)
            vendor = JvmVendorSpec.AMAZON // Amazon Corretto
        }

        compilerOptions {
            freeCompilerArgs =
                listOf(
                    // Opt-in to future Kotlin behavior, which gives .copy the same visibility as a data class constructor
                    "-Xconsistent-data-class-copy-visibility",
                    // Use JSR-305 @NotNull annotations for nullability if a library provides it
                    "-Xjsr305=strict",
                )
            javaParameters = true
        }
    }

    java {
        consistentResolution {
            // Disallow dependency versions from different dependency graphs to use different versions
            // This prevents situations like using a different version of libraries between runtime and test
            useCompileClasspathVersions()
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        testLogging {
            events =
                setOf(
                    org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                    org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
                    org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                )
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = true
        }
    }

    tasks.named<Test>("test") {
        useJUnitPlatform {
            excludeTags("integration")
        }
    }

    // Centrally configure the database configuration for use in all subprojects.
    configure<DatabaseConfiguration> {
        url = "jdbc:postgresql://localhost:8765/customs_product_service_develop?schema=public"
        username = "develop"
        password = "develop_secret"
    }

    dependencies {
        constraints {
            implementation("org.slf4j", "slf4j-api", "2.0.0") {
                because("Force SLF4J 2 over 1")
            }
        }
    }
}

// IntelliJ-IDEA specific configuration to subprojects
subprojects {
    apply(plugin = "org.gradle.idea")

    // Make javadoc and sources of libraries available in IntelliJ
    configure<org.gradle.plugins.ide.idea.model.IdeaModel> {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }
}

// Apply ktlint linting config to subprojects
val ktlintVersion = libs.versions.ktlint.get()
allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version = ktlintVersion
        enableExperimentalRules = false
        filter {
            exclude(
                "build/**/*.kt",
                "build/**/*.kts",
            )
        }
    }
}

// Apply kover code coverage to subprojects
subprojects {
    if (isGeneratedOrTemplateManaged) {
        return@subprojects
    }
    apply(plugin = "org.jetbrains.kotlinx.kover")

    kover {
        excludeTests {
            this.tasks("junitIntegrationTest")
        }
    }

    koverReport {
        defaults {
            xml {
                setReportFile(layout.buildDirectory.file("reports/kover/report.xml"))
            }
        }
    }
}

dependencies {
    // Make kover depend on each subproject
    // This will merge all Kover reports together so we get one coverage overview
    subprojects
        .filterNot { it.isGeneratedOrTemplateManaged }
        .forEach {
            kover(it)
        }
}

koverReport {
    filters {
        excludes {
            packages(
                // Start from customs-kotlin-service-full-template
                // generated jooq database code
                "com.flexport.customsproductservice.db",
                // End from customs-kotlin-service-full-template, you can add your app-specific exclusions below
            )
        }
    }

    defaults {
        xml {
            setReportFile(layout.buildDirectory.file("reports/kover/report.xml"))
        }
    }
}

project(":integration-test").tasks.named("test") {
    // We want to re-run integration tests even if they are up-to-date because changes
    // in the system under test affect the test results.
    outputs.upToDateWhen { false }
    onlyIf { project.findProperty("runIntegrationTest")?.toString()?.toBoolean() ?: false }
}

tasks.register("installGitHooks") {
    fun symlink(
        target: String,
        name: String,
    ) = arrayOf("ln", "-s", "-f", "../../$target", name).also {
        println(it.joinToString(" "))
    }
    doLast {
        println("Installing Git hooks...")
        exec {
            commandLine(*symlink("scripts/git-hooks/pre-commit", ".git/hooks/pre-commit"))
        }
        exec {
            commandLine(*symlink("scripts/git-hooks/prepare-commit-msg", ".git/hooks/prepare-commit-msg"))
        }
    }
}
