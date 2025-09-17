package net.thesilkminer.babelk.host

import net.thesilkminer.babelk.api.Logger
import net.thesilkminer.babelk.script.api.GrammarName
import net.thesilkminer.babelk.script.definition.GrammarScript
import net.thesilkminer.babelk.script.dsl.NamedObjectCollectionGetting
import net.thesilkminer.babelk.script.host.interop.ClassloadingCallback
import net.thesilkminer.babelk.script.host.interop.LoadingCallbacks
import net.thesilkminer.babelk.script.host.interop.LoadingScriptCollectionData
import net.thesilkminer.babelk.script.host.interop.LoadingScriptData
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.module.Configuration
import java.lang.module.ModuleDescriptor
import java.lang.module.ModuleFinder
import java.lang.module.ModuleReader
import java.lang.module.ModuleReference
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.security.CodeSigner
import java.security.CodeSource
import java.security.ProtectionDomain
import java.util.Collections
import java.util.Enumeration
import java.util.Optional
import java.util.stream.Stream
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.io.encoding.Base64
import kotlin.jvm.optionals.getOrElse
import kotlin.reflect.KClass
import kotlin.reflect.full.NoSuchPropertyException
import kotlin.streams.asStream

@OptIn(ExperimentalAtomicApi::class)
@Suppress("unused")
internal object HostLoadingCallbacks : LoadingCallbacks {
    private class ScriptModuleFinder(scripts: LoadingScriptCollectionData, id: Int) : ModuleFinder {
        private class ScriptModuleReference private constructor(
            descriptor: ModuleDescriptor,
            location: URI,
            private val readerCreator: () -> ModuleReader
        ) : ModuleReference(descriptor, location) {
            companion object {
                private class ScriptModuleReader(private val script: LoadingScriptData, private val baseLocation: URI) : ModuleReader {
                    override fun find(name: String?): Optional<URI> {
                        val uri = if (name !in this.script.allClassNames) null else URI.create("${this.baseLocation}?${name}")
                        return Optional.ofNullable(uri)
                    }

                    override fun open(name: String?): Optional<InputStream> {
                        return name?.let(this.script::classDataFor)
                            ?.let(::ByteArrayInputStream)
                            .let(Optional<InputStream>::ofNullable)
                    }

                    override fun list(): Stream<String> = this.script.allClassNames.asSequence().asStream()
                    override fun close() = Unit
                }

                private interface ModuleListDsl {
                    companion object {
                        private class ListBuilder(private val list: MutableList<String>) : ModuleListDsl {
                            override fun KClass<*>.unaryPlus() {
                                this@ListBuilder.list += this.java.module.name ?: error("${this.qualifiedName} is in the unnamed module")
                            }
                        }

                        fun moduleList(block: ModuleListDsl.() -> Unit): Collection<String> {
                            return buildList { ListBuilder(this).block() }
                        }
                    }

                    operator fun KClass<*>.unaryPlus()
                }

                private val modules by modules {
                    +GrammarName::class // script-api
                    +GrammarScript::class // script-definition
                    +NamedObjectCollectionGetting::class // script-dsl
                    +KClass::class // kotlin-stdlib
                    +NoSuchPropertyException::class // kotlin-reflect
                }

                fun forScript(script: LoadingScriptData, id: Int): ScriptModuleReference =
                    ScriptModuleReference(script.descriptor(id), script.location) { script.reader }

                private val LoadingScriptData.descriptor: (Int) -> ModuleDescriptor
                    get() = {
                        ModuleDescriptor.newModule(this.modularName)
                            .version("$it")
                            .requires(modules)
                            .exports(this.allPackages)
                            .build()
                    }

                private val LoadingScriptData.modularName: String
                    get() = this.grammarName.replace('/', '.')

                private val LoadingScriptData.location: URI
                    get() = URI.create("babelk-grammar:${this.grammarName}")

                private val LoadingScriptData.reader: ModuleReader
                    get() = ScriptModuleReader(this, this.location)

                private val LoadingScriptData.allPackages: Set<String>
                    get() = this.allClassNames.asSequence()
                        .filter { it.endsWith(".class") }
                        .map { it.substringBeforeLast('/').replace('/', '.') }
                        .toSet()

                private fun ModuleDescriptor.Builder.requires(modules: Iterable<String>): ModuleDescriptor.Builder =
                    modules.fold(this) { accumulator, module -> accumulator.requires(module) }

                private fun ModuleDescriptor.Builder.exports(packages: Iterable<String>): ModuleDescriptor.Builder =
                    packages.fold(this) { accumulator, pack -> accumulator.exports(pack) }

                private fun modules(block: ModuleListDsl.() -> Unit): Lazy<Iterable<String>> {
                    return lazy { ModuleListDsl.moduleList(block) }
                }
            }

            override fun open(): ModuleReader = this.readerCreator()
        }

        val allNames = scripts.map { it.grammarName }.toSet()
        private val references = scripts.associate { it.grammarName to ScriptModuleReference.forScript(it, id) }.toMap()

        override fun find(name: String?): Optional<ModuleReference> = name?.let { this.references[it] }.let(Optional<ModuleReference>::ofNullable)
        override fun findAll(): Set<ModuleReference> = this.references.values.toSet()
    }

    private class SimpleScriptClassLoader(id: Int, scripts: LoadingScriptCollectionData, parent: ClassLoader) : ClassLoader("grammars#$id", parent) {
        private class ClassData(val bytes: ByteArray, val grammarName: String) {
            operator fun component1(): ByteArray = this.bytes
            operator fun component2(): String = this.grammarName
        }

        private object GrammarStreamHandler : URLStreamHandler() {
            private class GrammarConnection(url: URL) : URLConnection(url) {
                override fun connect() = Unit
                override fun getInputStream(): InputStream = this.url.path.let(String::byteInputStream)
            }

            override fun openConnection(u: URL?): URLConnection? = u?.let(::GrammarConnection)
        }

        private object BytesStreamHandler : URLStreamHandler() {
            private class BytesConnection(url: URL) : URLConnection(url) {
                override fun connect() = Unit
                override fun getInputStream(): InputStream = this.url.path.let(Base64::decode).let(::ByteArrayInputStream)
            }

            override fun openConnection(u: URL?): URLConnection? = u?.let(::BytesConnection)
        }

        private val fullClassData = scripts.asSequence()
            .flatMap { it.allClassNames.map { name -> name to ClassData((it.classDataFor(name) ?: error("$name class data missing")), it.grammarName) } }
            .toMap()

        override fun findClass(name: String?): Class<*>? {
            val className = name?.replace('.', '/')?.plus(".class")
            val (bytes, grammar) = this.findClassData(className) ?: return super.findClass(name)
            val url = URL(null, "babelk-grammar:$grammar", GrammarStreamHandler)
            val source = CodeSource(url, arrayOf<CodeSigner>())
            val domain = ProtectionDomain(source, null)
            return this.defineClass(name, bytes, 0, bytes.count(), domain)
        }

        override fun getResourceAsStream(name: String?): InputStream? {
            return this.findClassData(name)
                ?.bytes
                ?.let(::ByteArrayInputStream) ?: super.getResourceAsStream(name)
        }

        override fun findResource(name: String?): URL? {
            return this.findClassData(name)
                ?.bytes
                ?.let(Base64::encode)
                ?.let { URL(null, "babelk-bytes:$it", BytesStreamHandler) } ?: super.findResource(name)
        }

        override fun findResources(name: String?): Enumeration<URL?>? {
            val parent = super.findResources(name)
            val thisLoader = this.findResource(name)
            val sequence = sequence {
                parent?.let {
                    while (it.hasMoreElements()) {
                        yield(it.nextElement())
                    }
                }
                thisLoader?.let { yield(it) }
            }
            return Collections.enumeration(sequence.toList())
        }

        private fun findClassData(name: String?): ClassData? {
            return if (name == null) null else this.fullClassData[name]
        }
    }

    private val logger = Logger {}
    private val layerId = AtomicInt(0)

    override fun setUpClassLoadingFor(scripts: LoadingScriptCollectionData): ClassloadingCallback {
        this.logger.info { "Setting up classloading for scripts $scripts, using $this" }

        val thisClass = this.javaClass
        val thisModule = thisClass.module
        val id = layerId.incrementAndFetch()
        val rootLoader = SimpleScriptClassLoader(id, scripts, thisClass.classLoader)

        if (!thisModule.isNamed) {
            this.logger.info { "  Environment is non-modular ($thisModule reports unnamed), skipping module integration" }
            return { _, name -> rootLoader.loadClass(name).kotlin }
        }

        this.logger.info { "  Environment is modular (name is reported as ${thisModule.name}), attempting full integration" }

        val thisLayer = thisModule.layer
        val thisConfiguration = thisLayer.configuration()
        val finder = ScriptModuleFinder(scripts, id)
        val allModules = finder.allNames
        val configuration = Configuration.resolveAndBind(finder, listOf(thisConfiguration), ModuleFinder.of(), allModules)
        val controller = ModuleLayer.defineModules(configuration, listOf(thisLayer)) { rootLoader }
        val layer = controller.layer()
        return { grammar, name ->
            val moduleName = grammar.replace('/', '.')
            val module = layer.findModule(moduleName).getOrElse { error("Grammar name $grammar was not found in layer $layer (module name: $moduleName)") }
            module.classLoader.loadClass(name).kotlin
        }
    }

    override fun toString(): String = "HostLoadingCallbacks (Java 9)"
}
