import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose)
    alias(libs.plugins.ksp)
}

group = "com.sanlorng.lib"

kotlin {
    jvmToolchain(17)
    jvm("desktop")
    androidTarget()
    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(project(":library"))
            }
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.activity)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "com.sanlorng.lib.sourcecodestring.example"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        applicationId = "com.sanlorng.lib.sourcecodestring.example"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

compose.desktop {
    application {
        mainClass = "com.sanlorng.lib.sourcecodestring.example.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.sanlorng.lib.sourcecodestring.example"
            packageVersion = "1.0.0"
        }
    }

}

ksp {
    val prefix = "SourceCodeString."
    arg(prefix + "inlineGetter", "false")
    arg(prefix + "getter", "false")
    arg(prefix + "packageName", "com.sanlorng.lib.generated")
    arg(prefix + "className", "Source")
    arg(prefix + "classAsObject", "false")
    arg(prefix + "extendProperty", "true")
    arg(prefix + "nameTemplate", "codeOf%s")
}

dependencies {
    val processor = project(":source-code-processor")
    add("kspCommonMainMetadata", processor)
}

// workaround for generating source code in Common Main.
// https://github.com/google/ksp/issues/567
tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().all {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}