# Dependency management

### Adding dependencies

Find the right gradle build file, usually `server/build.gradle.kts`, and add this to the `dependencies` section:

```kotlin
// Normal external dependency
implementation("group-id", "artifact-id", "version")

// On another gradle project
implementation(project(":path:to:project"))

// Test-only dependency
testImplementation("group-id", "artifact-id", "version")

// Dependency versions and bundles shared between projects
// see /settings.gradle.kts versionCatalogs
// and https://docs.gradle.org/current/userguide/platforms.html
testImplementation(libs.bundles.testing)
```

### Version conflict resolution

[Direct dependencies are chosen](https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.api.artifacts/-resolution-strategy/prefer-project-modules.html) over transitive dependencies so prevent invisible silent upgrades by transitive dependencies.
Transitive version conflicts will result in a build errors

Version conflicts can occur both at build time and runtime.
Telltale signs of a runtime version conflict are `NoClassDefFoundError`, `ClassNotFoundException`, `NoSuchMethodError` and `NoSuchMethodException`.

The following Gradle commands can be useful when debugging these problems:

```shell
# Shows the complete dependency tree of the server project
./gradlew server:dependencies

# Shows the version graph and resolution for the foo dependency (artifactId) in the server project
./gradle server:dependencyInsight --dependency foo
```

#### Resolving conflicts

Version conflicts can be solved in a few ways, in rough order of preference:

##### 1. Use dependencies with compatible versions

If possible, upgrade all conflicting dependencies so they use the same version, at least for the major version part.

```kotlin
// A 2.0.0 relies on shared 4.0.0
implementation("com.example", "A", "2.0.0")

// B 3.0.0 relies on shared 1.0.0 which causes a conflict
implementation("com.example", "B", "3.0.0")

// Fix: Upgrade B to a newer version which also uses shared 4.0.0
implementation("com.example", "B", "3.1.0")
```

##### 2. Specify a constraint

You can specify a minimum version that should be used for dependency resolution:

```kotlin
implementation("com.example", "A", "2.0.0")
implementation("com.example", "B", "3.0.0")
constraints {
    implementation("com.example", "shared", "4.0.0") {
        because("Use shared 4 over older versions")
    }
}
```

This is especially a good solution for dependencies that occur many times in the build graph.

##### 3. Exclude the problematic transitive dependency

You can exclude a specific transitive version:

```kotlin
implementation("com.example", "A", "2.0.0") {
    // A imports com.example:shared 4.0.0, but we cannot upgrade to that yet
    // because B doesn't work with it
    exclude("com.example", "shared")
}
```

This has as disadvantage that it can cause problems if `B` is removed and the transitive dependency is not included anymore.


##### 4. Provide a strict version

```kotlin
implementation("com.example", "A", "2.0.0")
implementation("com.example", "B", "3.0.0")
implementation("com.example", "shared") {
    version {
        strictly("4.0.0")
    }
}
```

This has the disadvantage that you add an extra direct dependency which you don't use,
and can easily be left behind if you remove the other dependencies.

It is also easy to leave an outdated version.

