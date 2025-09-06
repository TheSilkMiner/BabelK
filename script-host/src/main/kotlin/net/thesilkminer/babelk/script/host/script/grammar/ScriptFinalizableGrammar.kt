package net.thesilkminer.babelk.script.host.script.grammar

import net.thesilkminer.babelk.script.api.collection.MutableNamedObjectCollection
import net.thesilkminer.babelk.script.api.collection.NamedObjectCollection
import net.thesilkminer.babelk.script.api.collection.asReadOnlyView
import net.thesilkminer.babelk.script.api.grammar.Grammar
import net.thesilkminer.babelk.script.api.grammar.GrammarPack
import net.thesilkminer.babelk.script.api.grammar.NamedRule
import net.thesilkminer.babelk.script.api.grammar.Rule
import net.thesilkminer.babelk.script.api.grammar.RuleBuilderContext
import net.thesilkminer.babelk.script.api.grammar.ThisGrammar
import net.thesilkminer.babelk.script.api.provider.NamedObjectProvider
import net.thesilkminer.babelk.script.host.script.collection.GrammarRuleNamedObjectCollection

internal class ScriptFinalizableGrammar(private val grammarName: String, private val grammarPack: ScriptCollectionGrammarPack) : ThisGrammar {
    private class FinalizedCollection(private val collection: NamedObjectCollection<NamedRule>) : MutableNamedObjectCollection<NamedRule, Rule, RuleBuilderContext> {
        override fun byName(name: String): NamedObjectProvider<NamedRule> = this.collection.byName(name)
        override fun register(name: String, creator: RuleBuilderContext.() -> Rule): NamedObjectProvider<NamedRule> = error("Unavailable -- this should never be invoked!")
        override fun toString(): String = this.collection.toString()
    }

    private class WrapperPack(private val pack: ScriptCollectionGrammarPack) : GrammarPack {
        override val grammars: NamedObjectCollection<Grammar> get() = this.pack.grammars
        override fun toString(): String = this.grammars.toString()
    }

    private val rulesCollection = GrammarRuleNamedObjectCollection()
    private var isFinalized = false

    override val name: String get() = this.grammarName
    override val pack: GrammarPack get() = WrapperPack(this.grammarPack)
    override val rules: MutableNamedObjectCollection<NamedRule, Rule, RuleBuilderContext> get() = this.rulesCollection.finalizeIfNeeded()

    private fun MutableNamedObjectCollection<NamedRule, Rule, RuleBuilderContext>.finalizeIfNeeded(): MutableNamedObjectCollection<NamedRule, Rule, RuleBuilderContext> {
        return if (this@ScriptFinalizableGrammar.isFinalized) FinalizedCollection(this.asReadOnlyView()) else this
    }

    internal fun <R> finalize(beforeFinalizationCallback: ThisGrammar.() -> R): R {
        if (this.isFinalized) error("Already finalized -- this should never happen")
        val result = beforeFinalizationCallback()
        this.isFinalized = true
        return result
    }

    internal fun getRuleOrNull(name: String): NamedRule? {
        return this.rulesCollection.getOrNull(name)
    }

    internal fun getAllRules(): Collection<NamedRule> {
        return this.rulesCollection.getAllValues()
    }

    override fun toString(): String = "Grammar[name=${this.name},rules=${this.rules}]"
}
