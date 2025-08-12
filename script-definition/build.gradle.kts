plugins {
    id("net.thesilkminer.babelk.gradle.kotlin-conventions")
}

version = "0.1.0"

multiRelease {
    automaticModuleName = "net.thesilkminer.babelk.script.definition"
}

dependencies {
    implementation(kotlin("scripting-common"))
    implementation(kotlin("scripting-jvm"))

    implementation(project(":script-api"))
}
