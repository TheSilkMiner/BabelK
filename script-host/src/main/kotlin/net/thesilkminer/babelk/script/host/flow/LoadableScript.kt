package net.thesilkminer.babelk.script.host.flow

import kotlin.script.experimental.api.ScriptCompilationConfiguration

internal interface LoadableScript {
    val sourceLocationId: String?
    val compilationConfig: ScriptCompilationConfiguration
    val grammarName: String
    val mainClassName: String
    val allClassNames: Collection<String>
    fun classDataFor(name: String): ByteArray?
}
