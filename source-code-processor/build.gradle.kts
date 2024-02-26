plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "com.sanlorng.lib"
version = "0.0.1"

kotlin {
    jvmToolchain(17)
    jvm()
    sourceSets {
        jvmMain {
            dependencies {
                implementation(libs.ksp.processing)
                implementation(libs.ksp.processing.api)
                implementation(libs.kotlin.compiler)
                implementation(libs.kotlin.poet)
                implementation(project(":library"))
            }
        }
    }
}