@file:JvmName("CompilationHandler")

package net.thesilkminer.babelk.api

import net.thesilkminer.babelk.api.grammar.GrammarPack
import net.thesilkminer.babelk.api.script.ScriptBundle
import net.thesilkminer.babelk.compileScriptsToGrammarPack

fun ScriptBundle.compileToGrammarPack(): GrammarPack {
    return compileScriptsToGrammarPack(this)
}
