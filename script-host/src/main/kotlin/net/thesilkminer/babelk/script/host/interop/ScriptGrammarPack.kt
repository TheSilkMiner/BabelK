package net.thesilkminer.babelk.script.host.interop

interface ScriptGrammarPack {
    val allGrammars: Collection<ScriptGrammar>
    operator fun get(name: String): ScriptGrammar?
}
