plugins {
    `maven-publish`
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "com.flexport..customsproductservice"
    version = "0.0.1"

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            allWarningsAsErrors.set(true)
        }
    }

    val artifactoryPublishUrlBase = "https://artifactory.flexport.io/artifactory/"
    val defaultArtifactoryPublishRepo = "flexport-mvn-release-local"

    java {
        withSourcesJar()
    }

    kotlin {
        explicitApi()
    }

    publishing {
        publications {
            create<MavenPublication>(project.name) {
                from(components["java"])
                artifactId = "customsproductservice-kotlin-${project.name}"
            }
        }
        repositories {
            maven {
                url = uri(
                    artifactoryPublishUrlBase + (
                        System.getenv("ARTIFACTORY_REPOSITORY") ?: defaultArtifactoryPublishRepo
                        ),
                )
                credentials {
                    username = System.getenv("ARTIFACTORY_USERNAME")
                    password = System.getenv("ARTIFACTORY_PASSWORD")
                }
            }
        }
    }
}

// Don't publish this project, only the subprojects
tasks.withType<PublishToMavenLocal> {
    enabled = false
}
tasks.withType<PublishToMavenRepository> {
    enabled = false
}
