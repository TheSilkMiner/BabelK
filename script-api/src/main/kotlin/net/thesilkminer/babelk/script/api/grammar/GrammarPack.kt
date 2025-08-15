package net.thesilkminer.babelk.script.api.grammar

import net.thesilkminer.babelk.script.api.collection.NamedObjectCollection

interface GrammarPack {
    val grammars: NamedObjectCollection<Grammar>
}
