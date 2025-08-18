@file:JvmName("Host")

package net.thesilkminer.babelk.script.host

import net.thesilkminer.babelk.script.host.flow.compileAndEval
import net.thesilkminer.babelk.script.host.interop.LoadingCallbacks
import net.thesilkminer.babelk.script.host.interop.ScriptCollection
import net.thesilkminer.babelk.script.host.interop.ScriptGrammarPack

fun compileAndEvalCollection(
    collection: ScriptCollection,
    loadingCallbacks: LoadingCallbacks
): ScriptGrammarPack {
    return collection.compileAndEval(loadingCallbacks)
}
