package net.thesilkminer.babelk.script.host.interop

import net.thesilkminer.babelk.script.api.grammar.NamedRule

interface ScriptGrammar {
    val name: String
    val allRules: Collection<NamedRule>
    operator fun get(name: String): NamedRule?
}
