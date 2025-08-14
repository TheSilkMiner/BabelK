package net.thesilkminer.babelk.script.api.collection

import net.thesilkminer.babelk.script.api.NamedObject
import net.thesilkminer.babelk.script.api.provider.NamedObjectProvider

interface MutableNamedObjectCollection<E : NamedObject> : NamedObjectCollection<E> {
    fun register(name: String, creator: () -> E): NamedObjectProvider<E>
}
