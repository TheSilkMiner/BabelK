plugins {
    id("net.thesilkminer.babelk.gradle.kotlin-conventions")
    id("org.gradlex.extra-java-module-info") version "1.13"
}

version = "0.1.0"

extraJavaModuleInfo {
    deriveAutomaticModuleNamesFromFileNames = true
}

multiRelease {
    releases {
        release(9)
    }
}

dependencies {
    implementation(project(":library"))

    val java9Implementation by configurations.getting
    java9Implementation(kotlin("compiler-embeddable"))
    java9Implementation(kotlin("daemon-embeddable"))
    java9Implementation(kotlin("scripting-common"))
    java9Implementation(kotlin("scripting-compiler-embeddable"))
    java9Implementation(kotlin("scripting-compiler-impl-embeddable"))
    java9Implementation(kotlin("scripting-jvm"))
    java9Implementation(kotlin("scripting-jvm-host"))
    java9Implementation(kotlin("script-runtime"))
    java9Implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core-jvm", version = "1.8.0")
    java9Implementation(project(":script-api"))
    java9Implementation(project(":script-definition"))
    java9Implementation(project(":script-dsl"))
    java9Implementation(project(":script-host"))
}
