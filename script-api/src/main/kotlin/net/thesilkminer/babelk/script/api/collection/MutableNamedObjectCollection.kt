package net.thesilkminer.babelk.script.api.collection

import net.thesilkminer.babelk.script.api.NamedObject
import net.thesilkminer.babelk.script.api.provider.NamedObjectProvider

interface MutableNamedObjectCollection<out E : NamedObject, in B, out C : BuilderContext> : NamedObjectCollection<E> {
    fun register(name: String, creator: C.() -> B): NamedObjectProvider<E>
}
