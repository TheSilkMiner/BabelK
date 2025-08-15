package net.thesilkminer.babelk.script.api.collection

import net.thesilkminer.babelk.script.api.NamedObject
import net.thesilkminer.babelk.script.api.provider.NamedObjectProvider

interface MutableNamedObjectCollection<out E : NamedObject, in B> : NamedObjectCollection<E> {
    fun register(name: String, creator: () -> B): NamedObjectProvider<E>
}
