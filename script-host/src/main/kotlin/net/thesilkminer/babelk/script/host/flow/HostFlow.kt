@file:JvmName("HostFlow")

package net.thesilkminer.babelk.script.host.flow

import net.thesilkminer.babelk.script.host.flow.compilation.compile
import net.thesilkminer.babelk.script.host.interop.LoadingCallbacks
import net.thesilkminer.babelk.script.host.interop.ScriptCollection
import net.thesilkminer.babelk.script.host.interop.ScriptGrammarPack

internal fun ScriptCollection.compileAndEval(callbacks: LoadingCallbacks): ScriptGrammarPack {
    val loadableCollection = this.compile()
    TODO()
}
