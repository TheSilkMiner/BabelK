plugins {
    id("net.thesilkminer.babelk.gradle.kotlin-conventions")
    application
}

version = "0.1.0"

application {
    // TODO("Determine how to produce two distributions, one for J8 and one for J9+")
    mainModule = "net.thesilkminer.babelk.cli"
    mainClass = "net.thesilkminer.babelk.cli.Babelk"
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
