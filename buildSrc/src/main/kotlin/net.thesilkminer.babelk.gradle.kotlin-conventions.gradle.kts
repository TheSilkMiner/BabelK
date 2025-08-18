import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.extendsFrom
import java.time.LocalDateTime

plugins {
    id("net.neoforged.licenser")
    kotlin("jvm")
}

group = "net.thesilkminer.babelk"

kotlin {
    jvmToolchain(8)
    compilerOptions {
        this.moduleName = provider { "${project.group}.${project.name}" }
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

license {
    header(rootProject.file("NOTICE"))
    properties {
        set("year", LocalDateTime.now().year)
    }
    ignoreFailures = true
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
}

interface MultiReleaseSet {
    val name: String
    val version: Property<JavaLanguageVersion>
}

abstract class MultiReleaseExtension @Inject constructor(objects: ObjectFactory) {
    companion object {
        const val NAME = "multiRelease"
    }

    val automaticModuleName: Property<String> = objects.property(String::class)
    val releases: NamedDomainObjectContainer<MultiReleaseSet> = objects.domainObjectContainer(MultiReleaseSet::class)

    fun NamedDomainObjectContainer<MultiReleaseSet>.release(
        version: Int,
        action: Action<in MultiReleaseSet> = Action { this.version = JavaLanguageVersion.of(version) }
    ): NamedDomainObjectProvider<MultiReleaseSet> {
        return this.register("java$version", action)
    }
}

val mrExtension = extensions.create(MultiReleaseExtension.NAME, MultiReleaseExtension::class, objects)

tasks {
    mrExtension.releases.all {
        val set = sourceSets.create(this.name) // SourceSets aren't lazy anyway

        configurations.named(set.compileClasspathConfigurationName).extendsFrom(configurations.compileClasspath)
        dependencies.add(set.compileOnlyConfigurationName, sourceSets.main.get().output.classesDirs)

        tasks {
            named<JavaCompile>(set.compileJavaTaskName) {
                javaCompiler = javaToolchains.compilerFor { languageVersion = this@all.version }
            }
            named<KotlinCompile>(set.compileKotlinTaskName) {
                compilerOptions {
                    jvmTarget = JvmTarget.fromTarget(this@all.version.get().asInt().toString())
                    kotlinJavaToolchain.toolchain.use(javaToolchains.launcherFor { languageVersion = this@all.version })
                }
            }
            named<ProcessResources>(sourceSets.main.get().processResourcesTaskName) {
                from(set.output) {
                    into("META-INF/versions/${this@all.version.get().asInt()}")
                    exclude("META-INF/**")
                }
                from(set.output) {
                    include("META-INF/**")
                }
                dependsOn(set.output)
            }
        }
    }
}

afterEvaluate {
    tasks.named<Jar>("jar") {
        manifest {
            mrExtension.automaticModuleName.orNull?.let { attributes("Automatic-Module-Name" to it) }
            if (mrExtension.releases.isNotEmpty()) attributes("Multi-Release" to true)
        }
    }
}

private val SourceSet.compileKotlinTaskName: String get() = this.compileJavaTaskName.replace(Regex("Java$"), "Kotlin")
