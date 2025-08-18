@file:JvmName("Host")

package net.thesilkminer.babelk.script.host

import net.thesilkminer.babelk.script.host.interop.CompilationCallbacks
import net.thesilkminer.babelk.script.host.interop.ScriptCollection
import net.thesilkminer.babelk.script.host.interop.ScriptGrammarPack

private val log = Log {}

fun compileAndEvalCollection(
    collection: ScriptCollection,
    compilationCallbacks: CompilationCallbacks
): ScriptGrammarPack {
    TODO()
}
