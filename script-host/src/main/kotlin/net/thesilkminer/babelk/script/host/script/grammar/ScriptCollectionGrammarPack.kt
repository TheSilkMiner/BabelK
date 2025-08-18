package net.thesilkminer.babelk.script.host.script.grammar

import net.thesilkminer.babelk.script.api.collection.NamedObjectCollection
import net.thesilkminer.babelk.script.api.collection.asReadOnlyView
import net.thesilkminer.babelk.script.api.grammar.Grammar
import net.thesilkminer.babelk.script.api.grammar.GrammarPack
import net.thesilkminer.babelk.script.api.grammar.ThisGrammar
import net.thesilkminer.babelk.script.host.script.collection.GrammarNamedObjectCollection

internal class ScriptCollectionGrammarPack : GrammarPack {
    private val grammarCollection = GrammarNamedObjectCollection()
    override val grammars: NamedObjectCollection<Grammar> get() = this.grammarCollection.asReadOnlyView()
    override fun toString(): String = "GrammarPack[grammars=${this.grammars}]"

    internal fun prepareNewGrammar(name: String) {
        this.grammarCollection.register(name) { ScriptFinalizableGrammar(name, this) }
    }

    internal fun finalizeGrammar(name: String, block: ThisGrammar.() -> Unit) {
        val grammar = this.getGrammarOrNull(name) ?: error("Grammar $name cannot be finalized as it was never prepared")
        grammar.finalize(block)
    }

    internal fun getGrammarOrNull(name: String): ScriptFinalizableGrammar? {
        return this.grammarCollection.getOrNull(name) as ScriptFinalizableGrammar?
    }
}
