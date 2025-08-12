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
    implementation(kotlin("compiler-embeddable"))
    implementation(kotlin("daemon-embeddable"))
    implementation(kotlin("scripting-common"))
    implementation(kotlin("scripting-compiler-embeddable"))
    implementation(kotlin("scripting-compiler-impl-embeddable"))
    implementation(kotlin("scripting-jvm"))
    implementation(kotlin("scripting-jvm-host"))
    implementation(kotlin("script-runtime"))

    implementation(libs.kotlin.logging.jvm)

    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core-jvm", version = "1.8.0")

    implementation(project(":script-api"))
    implementation(project(":script-definition"))
    implementation(project(":script-dsl"))
    implementation(project(":script-host"))
}
