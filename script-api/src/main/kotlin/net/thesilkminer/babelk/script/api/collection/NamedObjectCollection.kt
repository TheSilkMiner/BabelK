package net.thesilkminer.babelk.script.api.collection

import net.thesilkminer.babelk.script.api.NamedObject
import net.thesilkminer.babelk.script.api.provider.NamedObjectProvider

interface NamedObjectCollection<out E : NamedObject> {
    fun byName(name: String): NamedObjectProvider<E>
}