package net.thesilkminer.babelk.script.api.invoke

import kotlin.reflect.KClass

data class RuleStateKey<T : Any>(val id: String, val type: KClass<T>)
