@file:JvmName("GrammarWrappers")

package net.thesilkminer.babelk.host

import net.thesilkminer.babelk.api.grammar.Grammar
import net.thesilkminer.babelk.api.grammar.GrammarPack
import net.thesilkminer.babelk.api.grammar.GrammarRule
import net.thesilkminer.babelk.api.invoke.RandomSource
import net.thesilkminer.babelk.api.invoke.RuleInvocationSequence
import net.thesilkminer.babelk.api.invoke.RuleInvocationSequenceType
import net.thesilkminer.babelk.script.api.grammar.NamedRule
import net.thesilkminer.babelk.script.host.interop.ScriptGrammar
import net.thesilkminer.babelk.script.host.interop.ScriptGrammarPack

private class HostProvidedNamedGrammarRule(private val rule: NamedRule) : GrammarRule {
    override val name: String get() = this.rule.name

    override fun invoke(rng: RandomSource, sequenceType: RuleInvocationSequenceType, arguments: Map<String, String>): RuleInvocationSequence {
        TODO("Not yet implemented")
    }

    override fun toString(): String = this.rule.toString()
}

private class HostProvidedGrammar(private val grammar: ScriptGrammar) : Grammar {
    override val name: String get() = this.grammar.name
    override val rules: Collection<GrammarRule> get() = this.grammar.allRules.map(::HostProvidedNamedGrammarRule)
    override fun findRule(name: String): GrammarRule? = this.grammar[name]?.let(::HostProvidedNamedGrammarRule)
    override fun toString(): String = this.grammar.toString()
}

private class HostProvidedGrammarPack(private val pack: ScriptGrammarPack) : GrammarPack {
    override val grammars: Collection<Grammar> get() = this.pack.allGrammars.map(::HostProvidedGrammar)
    override fun findGrammar(name: String): Grammar? = this.pack[name]?.let(::HostProvidedGrammar)
    override fun toString(): String = this.pack.toString()
}

internal fun ScriptGrammarPack.toGrammarPack(): GrammarPack = HostProvidedGrammarPack(this)
