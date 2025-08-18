package net.thesilkminer.babelk.script.host.script.collection

import net.thesilkminer.babelk.script.api.NamedObject
import net.thesilkminer.babelk.script.api.collection.MutableNamedObjectCollection
import net.thesilkminer.babelk.script.api.provider.NamedObjectProvider

internal abstract class MapBackedMutableNamedObjectCollection<E : NamedObject, in B> : MutableNamedObjectCollection<E, B> {
    protected class SimpleNamedObjectProvider<T : NamedObject>(override val name: String, private val obj: T) : NamedObjectProvider<T> {
        override fun get(): T = this.obj
        override fun toString(): String = "Provider[${this.name}=${this.obj}]"
    }

    private val map = mutableMapOf<String, E>()

    final override fun register(name: String, creator: () -> B): NamedObjectProvider<E> {
        require(name !in this.map) { "An object was already registered for name '$name' in the current collection: ${this.getOrNull(name)}" }
        val obj = creator().toNamedObject(name)
        this.map[name] = obj
        return this.providerForObject(name, obj)
    }

    final override fun byName(name: String): NamedObjectProvider<E> {
        val inMap = this.getOrNull(name)
        if (inMap != null) {
            return this.providerForObject(name, inMap)
        }

        val attemptLookup = this.providerForLookup(name) { this.getOrNull(name) ?: this.noElementFound(name, true) }
        if (attemptLookup != null) {
            return attemptLookup
        }

        this.noElementFound(name, false)
    }

    protected abstract fun B.toNamedObject(name: String): E
    protected abstract fun providerForObject(name: String, obj: E): NamedObjectProvider<E>
    protected abstract fun providerForLookup(name: String, lookup: () -> E): NamedObjectProvider<E>?

    internal fun getOrNull(name: String): E? = this.map[name]

    private fun noElementFound(name: String, delayedLookup: Boolean): Nothing {
        val delayedLookupExplanation = if (delayedLookup) "" else " and delayed lookup is not supported"
        throw NoSuchElementException("No object named '$name' was found in the current collection$delayedLookupExplanation")
    }

    override fun toString(): String = "NamedObjectCollection[${this.map.keys}]"
}
