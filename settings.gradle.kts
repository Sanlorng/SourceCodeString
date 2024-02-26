pluginManagement {
    includeBuild("convention-plugins")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SourceCodeString"
includeBuild("BuildPlugin")
include(":library")
include("source-code-processor")
include("example")
