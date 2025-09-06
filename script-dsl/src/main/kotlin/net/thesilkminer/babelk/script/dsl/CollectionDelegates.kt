@file:JvmName("CollectionDelegates")

package net.thesilkminer.babelk.script.dsl

import net.thesilkminer.babelk.script.api.NamedObject
import net.thesilkminer.babelk.script.api.collection.BuilderContext
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

private class NamedObjectCollectionRegisteringDelegate<out E : NamedObject, in B, out C : BuilderContext>(
    private val collection: MutableNamedObjectCollection<E, B, C>,
    private val name: String?,
    private val creator: C.() -> B
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

fun <E : NamedObject, B, C : BuilderContext> MutableNamedObjectCollection<E, B, C>.registering(
    name: String? = null,
    creator: C.() -> B
): NamedObjectCollectionRegistering<E> {
    return NamedObjectCollectionRegisteringDelegate(this, name, creator)
}
