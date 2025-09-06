package net.thesilkminer.babelk.script.api.grammar

import net.thesilkminer.babelk.script.api.collection.MutableNamedObjectCollection

interface ThisGrammar : Grammar {
    override val rules: MutableNamedObjectCollection<NamedRule, Rule, RuleBuilderContext>
    val pack: GrammarPack
}
