plugins {
    id("net.thesilkminer.babelk.gradle.kotlin-conventions")
}

version = "0.1.0"

multiRelease {
    releases {
        release(9)
    }
}

dependencies {
    implementation(project(":script-api"))
    implementation(project(":script-host"))

    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core-jvm", version = "1.8.0")

    // Java 9 source set requires access to those two automatic modules to properly set up module requires clauses,
    // so provide them without exposing them to the general library as it doesn't need it
    val java9Implementation by configurations.getting
    java9Implementation(project(":script-definition"))
    java9Implementation(project(":script-dsl"))
}

tasks.named<JavaCompile>(sourceSets["java9"].compileJavaTaskName) {
    sourceSets.main
        .map { it.output.classesDirs.files.joinToString(prefix = "${project.group}=", separator = File.pathSeparator) }
        .let { options.compilerArgumentProviders.add { listOf("--patch-module", it.get()) }}
}
