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
    implementation(project(":java9-modular-adapter"))
    implementation(project(":script-api"))
    implementation(project(":script-host"))
}

tasks.named<JavaCompile>(sourceSets["java9"].compileJavaTaskName) {
    sourceSets.main
        .map { it.output.classesDirs.files.joinToString(prefix = "${project.group}=", separator = File.pathSeparator) }
        .let { options.compilerArgumentProviders.add { listOf("--patch-module", it.get()) }}
}
