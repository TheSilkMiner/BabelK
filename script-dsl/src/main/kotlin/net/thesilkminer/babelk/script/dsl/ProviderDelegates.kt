@file:JvmName("ProviderDelegates")

package net.thesilkminer.babelk.script.dsl

import net.thesilkminer.babelk.script.api.provider.Provider
import kotlin.reflect.KProperty

operator fun <T> Provider<T>.getValue(thisRef: Any?, property: KProperty<*>): T = this.get()
