plugins {
    id("net.thesilkminer.babelk.gradle.kotlin-conventions")
    application
}

version = "0.1.0"

application {
    // TODO("Determine how to produce two distributions, one for J8 and one for J9+")
    mainModule = "net.thesilkminer.babelk.cli"
    mainClass = "net.thesilkminer.babelk.cli.Babelk"
    applicationDefaultJvmArgs = listOf("--add-modules=jdk.unsupported")
}

multiRelease {
    releases {
        release(9)
    }
}

dependencies {
    implementation(project(":library"))
    implementation(libs.picocli)
}

tasks.named<JavaCompile>(sourceSets["java9"].compileJavaTaskName) {
    options.javaModuleVersion = "${project.version}"
    sourceSets.main
        .map { it.output.classesDirs.files.joinToString(prefix = "${project.group}.cli=", separator = File.pathSeparator) }
        .let { options.compilerArgumentProviders.add { listOf("--patch-module", it.get()) }}
}

tasks.named<ProcessResources>("processResources") {
    val version = project.version
    filesMatching("META-INF/babelk.version") {
        expand("version" to version)
    }
}
