@file:JvmName("CompiledScriptInternalsBatteringRam")

package net.thesilkminer.babelk.script.host.hack

import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.jvm.impl.KJvmCompiledModule
import kotlin.script.experimental.jvm.impl.KJvmCompiledModuleInMemory
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript

internal interface CompiledScriptEntrails {
    val scriptClassFQName: String
    val compilerOutputFiles: Map<String, ByteArray>
}

private class CompiledScriptExposedEntrails(private val script: CompiledScript) : CompiledScriptEntrails {
    override val scriptClassFQName: String get() = this.script.scriptClassFQName
    override val compilerOutputFiles: Map<String, ByteArray> get() = this.script.compilerOutputFiles
}

internal fun CompiledScript.batteringRam(): CompiledScriptEntrails = CompiledScriptExposedEntrails(this)

private val CompiledScript.scriptClassFQName: String get() = this.jvm.scriptClassFQName
private val CompiledScript.compilerOutputFiles: Map<String, ByteArray> get() = this.jvm.module.memory.compilerOutputFiles

private val CompiledScript.jvm: KJvmCompiledScript get() = this as? KJvmCompiledScript ?: entrailsError()
private val KJvmCompiledScript.module: KJvmCompiledModule get() = this.jvm.getCompiledModule() ?: entrailsError()
private val KJvmCompiledModule.memory: KJvmCompiledModuleInMemory get() = this as? KJvmCompiledModuleInMemory ?: entrailsError()

private fun entrailsError(): Nothing = throw IllegalStateException("Unable to access entrails")
