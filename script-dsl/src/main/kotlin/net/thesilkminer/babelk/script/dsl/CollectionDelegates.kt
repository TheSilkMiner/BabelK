@file:JvmName("CollectionDelegates")

package net.thesilkminer.babelk.script.dsl

import net.thesilkminer.babelk.script.api.NamedObject
import net.thesilkminer.babelk.script.api.collection.MutableNamedObjectCollection
import net.thesilkminer.babelk.script.api.collection.NamedObjectCollection
import net.thesilkminer.babelk.script.api.provider.Provider
import kotlin.properties.PropertyDelegateProvider
import kotlin.reflect.KProperty

interface NamedObjectCollectionGetting<out E : NamedObject> : PropertyDelegateProvider<Any?, Provider<E>>
interface NamedObjectCollectionRegistering<out E : NamedObject> : PropertyDelegateProvider<Any?, Provider<E>>

private class NamedObjectCollectionGettingDelegate<out E : NamedObject>(
    private val collection: NamedObjectCollection<E>,
    private val name: String?
) : NamedObjectCollectionGetting<E> {
    override operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): Provider<E> = this.collection.byName(this.name ?: property.name)
}

private class NamedObjectCollectionRegisteringDelegate<out E : NamedObject, in B>(
    private val collection: MutableNamedObjectCollection<E, B>,
    private val name: String?,
    private val creator: () -> B
) : NamedObjectCollectionRegistering<E> {
    override operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): Provider<E> = this.collection.register(this.name ?: property.name, this.creator)
}

operator fun <E : NamedObject> NamedObjectCollection<E>.provideDelegate(thisRef: Any?, property: KProperty<*>): Provider<E> {
    return this.getting.provideDelegate(thisRef, property)
}

val <E : NamedObject> NamedObjectCollection<E>.getting: NamedObjectCollectionGetting<E>
    get() = this.getting()

fun <E : NamedObject> NamedObjectCollection<E>.getting(name: String? = null): NamedObjectCollectionGetting<E> {
    return NamedObjectCollectionGettingDelegate(this, name)
}

fun <E : NamedObject, B> MutableNamedObjectCollection<E, B>.registering(name: String? = null, creator: () -> B): NamedObjectCollectionRegistering<E> {
    return NamedObjectCollectionRegisteringDelegate(this, name, creator)
}
