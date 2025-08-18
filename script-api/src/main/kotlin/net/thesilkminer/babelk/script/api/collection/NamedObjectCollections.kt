@file:JvmName("NamedObjectCollections")

package net.thesilkminer.babelk.script.api.collection

import net.thesilkminer.babelk.script.api.NamedObject
import net.thesilkminer.babelk.script.api.provider.NamedObjectProvider

private class ReadOnlyViewNamedObjectCollection<E : NamedObject>(private val collection: NamedObjectCollection<E>) : NamedObjectCollection<E> {
    override fun byName(name: String): NamedObjectProvider<E> = this.collection.byName(name)
    override fun toString(): String = this.collection.toString()
}

fun <E : NamedObject> NamedObjectCollection<E>.asReadOnlyView(): NamedObjectCollection<E> {
    return ReadOnlyViewNamedObjectCollection(this)
}
