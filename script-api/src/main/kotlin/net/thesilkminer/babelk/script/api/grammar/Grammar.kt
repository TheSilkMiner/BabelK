package net.thesilkminer.babelk.script.api.grammar

import net.thesilkminer.babelk.script.api.NamedObject
import net.thesilkminer.babelk.script.api.collection.NamedObjectCollection

interface Grammar : NamedObject {
    val rules: NamedObjectCollection<NamedRule>
}
