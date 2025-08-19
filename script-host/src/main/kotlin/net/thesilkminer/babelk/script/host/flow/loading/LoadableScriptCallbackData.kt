package net.thesilkminer.babelk.script.host.flow.loading

import net.thesilkminer.babelk.script.host.flow.LoadableScript
import net.thesilkminer.babelk.script.host.interop.LoadingScriptData

internal class LoadableScriptCallbackData(private val script: LoadableScript) : LoadingScriptData {
    override val grammarName: String get() = this.script.grammarName
    override val allClassNames: Collection<String> get() = this.script.allClassNames
    override fun classDataFor(name: String): ByteArray? = this.script.classDataFor(name)
}
