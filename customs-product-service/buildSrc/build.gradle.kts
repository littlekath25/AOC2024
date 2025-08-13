plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

kotlin {
    // This also sets Java toolchain version
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.AMAZON // Amazon Corretto
    }
}
