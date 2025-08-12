pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
}

rootProject.name = "babelk"

include("script-api")
include("script-dsl")
include("script-definition")
include("script-host")
include("library")
include("java9-modular-adapter")
