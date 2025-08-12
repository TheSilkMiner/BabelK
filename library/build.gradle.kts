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
    implementation(libs.kotlin.logging.jvm)

    implementation(project(":java9-modular-adapter"))
    implementation(project(":script-api"))
    implementation(project(":script-host"))
}
