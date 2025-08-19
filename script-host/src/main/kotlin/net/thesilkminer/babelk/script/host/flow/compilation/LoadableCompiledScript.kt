package net.thesilkminer.babelk.script.host.flow.compilation

import net.thesilkminer.babelk.script.host.flow.LoadableScript
import net.thesilkminer.babelk.script.host.flow.nameFromGrammarClass
import net.thesilkminer.babelk.script.host.hack.batteringRam
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration

internal class LoadableCompiledScript(private val compiledScript: CompiledScript) : LoadableScript {
    private val entrails = this.compiledScript.batteringRam()

    override val sourceLocationId: String? get() = this.compiledScript.sourceLocationId
    override val compilationConfig: ScriptCompilationConfiguration get() = this.compiledScript.compilationConfiguration
    override val grammarName: String get() = this.mainClassName.nameFromGrammarClass()
    override val mainClassName: String get() = this.entrails.scriptClassFQName
    override val allClassNames: Collection<String> get() = this.entrails.compilerOutputFiles.keys
    override fun classDataFor(name: String): ByteArray? = this.entrails.compilerOutputFiles[name]
}
