@file:JvmName("InteroperabilityWrapper")

package net.thesilkminer.babelk.script.host

import net.thesilkminer.babelk.script.api.grammar.NamedRule
import net.thesilkminer.babelk.script.host.interop.ScriptGrammar
import net.thesilkminer.babelk.script.host.interop.ScriptGrammarPack
import net.thesilkminer.babelk.script.host.script.grammar.ScriptCollectionGrammarPack
import net.thesilkminer.babelk.script.host.script.grammar.ScriptFinalizableGrammar

private class ScriptGrammarAdapter(private val grammar: ScriptFinalizableGrammar) : ScriptGrammar {
    override val name: String get() = this.grammar.name
    override val allRules: Collection<NamedRule> get() = this.grammar.getAllRules()
    override fun get(name: String): NamedRule? = this.grammar.getRuleOrNull(name)
    override fun toString(): String = this.grammar.toString()
}

private class ScriptGrammarPackAdapter(private val pack: ScriptCollectionGrammarPack) : ScriptGrammarPack {
    override val allGrammars: Collection<ScriptGrammar> get() = this.pack.getAllGrammars().map(::ScriptGrammarAdapter)
    override fun get(name: String): ScriptGrammar? = this.pack.getGrammarOrNull(name)?.let(::ScriptGrammarAdapter)
    override fun toString(): String = this.pack.toString()
}

internal fun ScriptCollectionGrammarPack.toInteroperableDefinition(): ScriptGrammarPack = ScriptGrammarPackAdapter(this)
