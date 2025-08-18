plugins {
    id("net.thesilkminer.babelk.gradle.kotlin-conventions")
}

version = "0.1.0"

multiRelease {
    automaticModuleName = "net.thesilkminer.babelk.script.host"
}

dependencies {
    implementation(kotlin("scripting-common"))
    implementation(kotlin("scripting-jvm"))
    implementation(kotlin("scripting-jvm-host"))

    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core-jvm", version = "1.8.0")

    implementation(project(":script-api"))
    implementation(project(":script-definition"))
    implementation(project(":script-dsl"))
}
