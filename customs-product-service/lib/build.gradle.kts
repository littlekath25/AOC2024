subprojects {
    apply(plugin = "java-library")

    group = "com.flexport.customs.lib"

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            // TODO: We likely want this for the other gradle projects too
            //  but this needs coordination with all template users
            allWarningsAsErrors.set(true)
        }
    }
}
