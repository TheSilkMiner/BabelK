package net.thesilkminer.babelk.script.host.script.collection

import net.thesilkminer.babelk.script.api.collection.NamedObjectCollection
import net.thesilkminer.babelk.script.api.collection.asReadOnlyView
import net.thesilkminer.babelk.script.api.grammar.Grammar
import net.thesilkminer.babelk.script.api.grammar.NamedRule
import net.thesilkminer.babelk.script.api.grammar.ThisGrammar
import net.thesilkminer.babelk.script.api.provider.NamedObjectProvider

internal class GrammarNamedObjectCollection : MapBackedMutableNamedObjectCollection<Grammar, ThisGrammar>() {
    private class ReadOnlyGrammar(private val grammar: Grammar): Grammar {
        override val name: String get() = this.grammar.name
        override val rules: NamedObjectCollection<NamedRule> get() = this.grammar.rules.asReadOnlyView()
        override fun toString(): String = this.grammar.toString()
    }

    override fun ThisGrammar.toNamedObject(name: String): Grammar = this
    override fun providerForObject(name: String, obj: Grammar): NamedObjectProvider<Grammar> = SimpleNamedObjectProvider(name, obj.ensureReadOnly())
    override fun providerForLookup(name: String, lookup: () -> Grammar): NamedObjectProvider<Grammar>? = null

    private fun Grammar.ensureReadOnly(): Grammar = ReadOnlyGrammar(this)
}
