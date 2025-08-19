package net.thesilkminer.babelk.host

import net.thesilkminer.babelk.script.host.interop.ClassloadingCallback
import net.thesilkminer.babelk.script.host.interop.LoadingCallbacks
import net.thesilkminer.babelk.script.host.interop.LoadingScriptCollectionData
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.security.CodeSigner
import java.security.CodeSource
import java.security.ProtectionDomain
import java.util.Collections
import java.util.Enumeration
import kotlin.io.encoding.Base64

internal object HostLoadingCallbacks : LoadingCallbacks {
    private class SimpleScriptClassLoader(scripts: LoadingScriptCollectionData, parent: ClassLoader) : ClassLoader(parent) {
        private class ClassData(val bytes: ByteArray, val grammarName: String) {
            operator fun component1(): ByteArray = this.bytes
            operator fun component2(): String = this.grammarName
        }

        private object GrammarStreamHandler : URLStreamHandler() {
            private class GrammarConnection(url: URL) : URLConnection(url) {
                override fun connect() = Unit
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

    override fun setUpClassLoadingFor(scripts: LoadingScriptCollectionData): ClassloadingCallback {
        val classLoader = SimpleScriptClassLoader(scripts, this.javaClass.classLoader)
        return { _, name -> classLoader.loadClass(name).kotlin }
    }

    override fun toString(): String = "HostLoadingCallbacks"
}
