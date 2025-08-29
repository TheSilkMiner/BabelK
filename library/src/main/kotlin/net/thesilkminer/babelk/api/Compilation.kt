@file:JvmName("CompilationHandler")

package net.thesilkminer.babelk.api

import net.thesilkminer.babelk.api.grammar.GrammarPack
import net.thesilkminer.babelk.api.script.ScriptBundle
import net.thesilkminer.babelk.compileScriptsToGrammarPack

class ScriptCompilationException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable?) : super(message, cause)
}

@Throws(ScriptCompilationException::class)
fun ScriptBundle.compileToGrammarPack(): GrammarPack {
    return compileScriptsToGrammarPack(this)
}
