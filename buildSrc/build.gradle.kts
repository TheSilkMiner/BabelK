plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases/")
}

dependencies {
    gradleApi()
    implementation(group = "org.jetbrains.kotlin.jvm", name = "org.jetbrains.kotlin.jvm.gradle.plugin", version = "2.2.0")
    implementation(group = "net.neoforged.licenser", name = "net.neoforged.licenser.gradle.plugin", version = "0.7.5")
}
