@file:JvmName("InvocationUtilities")

package net.thesilkminer.babelk.script.api.invoke

private object EmptyInvocationArguments : InvocationArguments {
    override val allArguments: Set<ArgumentName> = setOf()
    override fun argumentValue(name: ArgumentName): InvokableRule? = null
    override fun toString(): String = "()"
}

// Use String to avoid boxing and unboxing
private class MapBackedInvocationArguments(private val map: Map<String, InvokableRule>) : InvocationArguments {
    override val allArguments: Set<ArgumentName> get() = this.map.keys.mapTo(mutableSetOf(), ::ArgumentName)
    override fun argumentValue(name: ArgumentName): InvokableRule? = this.map[name.name]
    override fun toString(): String = "(${this.map.entries.joinToString { (key, value) -> "$key = $value" }})"
}

operator fun InvocationArguments.get(name: ArgumentName): InvokableRule? = this.argumentValue(name)

inline fun InvocationArguments.argumentOrElse(name: ArgumentName, default: () -> InvokableRule): InvokableRule = this.argumentValue(name) ?: default()
inline fun InvocationArguments.getOrElse(name: ArgumentName, default: () -> InvokableRule): InvokableRule = this.argumentOrElse(name, default)

fun InvocationArguments.getValue(name: ArgumentName): InvokableRule =
    this.getOrElse(name) { error("Expected argument for name $name, but only ${this.allArguments} available") }

fun InvocationArguments(vararg pairs: Pair<ArgumentName, InvokableRule>): InvocationArguments {
    return if (pairs.isEmpty()) EmptyInvocationArguments else MapBackedInvocationArguments(pairs.toMapNoDuplicates())
}

private fun Array<out Pair<ArgumentName, InvokableRule>>.toMapNoDuplicates(): Map<String, InvokableRule> {
    require(this.isNotEmpty()) { "Empty array conversion to map should never occur" }
    val count = this.count()
    if (count == 1) {
        val (key, value) = this[0]
        return mapOf(key.name to value)
    }

    val map = LinkedHashMap<String, InvokableRule>(count)
    for ((key, value) in this) {
        val oldValue = map.putIfAbsent(key.name, value)
        require(oldValue == null) { "Found duplicate argument $key (mapped to both $value and $oldValue)" }
    }
    return map.toMap()
}

inline fun <reified T : Any> RuleStateKey(id: String): RuleStateKey<T> = RuleStateKey(id, T::class)
