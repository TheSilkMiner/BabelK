@file:JvmName("HostCompilationFlow")

package net.thesilkminer.babelk.host

import net.thesilkminer.babelk.api.Logger
import net.thesilkminer.babelk.api.grammar.GrammarPack
import net.thesilkminer.babelk.api.script.ScriptBundle
import net.thesilkminer.babelk.script.host.compileAndEvalCollection
import net.thesilkminer.babelk.script.host.interop.ScriptCollection
import net.thesilkminer.babelk.script.host.interop.ScriptGrammarPack

private val logger = Logger {}

internal fun ScriptBundle.compile(): GrammarPack {
    logger.debug { "Setting up scaffolding for bundle compilation for $this" }
    val collection = this.toScriptCollection()
    val scriptPack = collection.compileAndEval()
    val pack = scriptPack.toGrammarPack()
    logger.info { "Successfully compiled and loaded grammar pack containing ${pack.grammars.count()} grammars" }
    logger.debug { "  Pack data: $pack" }
    return pack
}

private fun ScriptCollection.compileAndEval(): ScriptGrammarPack {
    logger.info { "Compiling and evaluating script collection" }
    return compileAndEvalCollection(this, HostLoadingCallbacks)
}
