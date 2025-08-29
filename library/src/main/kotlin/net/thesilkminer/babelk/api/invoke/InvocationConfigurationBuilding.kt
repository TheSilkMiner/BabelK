@file:JvmName("InvocationConfigurationBuilding")

package net.thesilkminer.babelk.api.invoke

import java.util.function.Consumer

private const val WRITE_ONLY_PROPERTY = "This property can only be written to and its value cannot be read directly within the DSL"

interface SequenceConfigurationDsl {
    @get:Deprecated(level = DeprecationLevel.HIDDEN, message = WRITE_ONLY_PROPERTY)
    @get:JvmSynthetic
    var type: RuleInvocationSequenceType

    @get:Deprecated(level = DeprecationLevel.HIDDEN, message = WRITE_ONLY_PROPERTY)
    @get:JvmSynthetic
    var stackDepth: Int
}

interface InvocationConfigurationDsl {
    @get:Deprecated(level = DeprecationLevel.HIDDEN, message = WRITE_ONLY_PROPERTY)
    @get:JvmSynthetic
    var randomSource: RandomSource

    @JvmSynthetic fun sequence(block: SequenceConfigurationDsl.() -> Unit)
    fun sequence(block: Consumer<SequenceConfigurationDsl>)

    fun arguments(arguments: Map<String, String>)
    fun arguments(block: MutableMap<String, String>.() -> Unit)
    fun arguments(vararg arguments: Pair<String, String>)
}

interface InvocationConfigurationBuilder : InvocationConfigurationDsl {
    fun build(): InvocationConfiguration
}

@Suppress("OVERRIDE_DEPRECATION")
private class ConfigurationBuilder : InvocationConfigurationBuilder, SequenceConfigurationDsl {
    private class DuplicateCheckingMutableMap<K, V>(private val delegate: MutableMap<K, V>) : MutableMap<K, V> by delegate {
        private class DuplicateCheckingMutableSet<K, V>(
            private val delegate: MutableSet<MutableMap.MutableEntry<K, V>>
        ) : MutableSet<MutableMap.MutableEntry<K, V>> by delegate {
            override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
                if (element in this.delegate) return false
                if (this.delegate.any { (key) -> key == element.key }) return false
                return this.delegate.add(element)
            }

            override fun addAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
                return elements.fold(false) { acc, entry -> this.add(entry) || acc }
            }

            override fun equals(other: Any?): Boolean = this.delegate == other
            override fun hashCode(): Int = this.delegate.hashCode()
            override fun toString(): String = "${this.delegate}"
        }

        override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = DuplicateCheckingMutableSet(this.delegate.entries)

        override fun put(key: K, value: V): V? {
            val result = this.putIfAbsent(key, value)
            if (result != null) {
                error("Attempted to override value for argument '$key': this is not supported (old: '$result', new '$value')")
            }
            return result
        }

        override fun putAll(from: Map<out K, V>) {
            from.forEach { (key, value) -> this[key] = value }
        }

        override fun equals(other: Any?): Boolean = this.delegate == other
        override fun hashCode(): Int = this.delegate.hashCode()
        override fun toString(): String = "${this.delegate}"
    }

    private data class Configuration(
        override val randomSource: RandomSource,
        override val ruleArguments: Map<String, String>,
        override val stackDepth: Int,
        override val type: RuleInvocationSequenceType
    ) : InvocationConfiguration, InvocationConfiguration.SequenceConfiguration {
        override val sequenceConfiguration: InvocationConfiguration.SequenceConfiguration get() = this
    }

    override var type: RuleInvocationSequenceType = RuleInvocationSequenceType.LIGHTWEIGHT
    override var stackDepth: Int = Int.MAX_VALUE
    override var randomSource: RandomSource = RandomSource

    private val arguments: MutableMap<String, String> = mutableMapOf()

    override fun sequence(block: SequenceConfigurationDsl.() -> Unit) {
        this.block()
    }

    override fun sequence(block: Consumer<SequenceConfigurationDsl>) {
        this.sequence { block.accept(this) }
    }

    override fun arguments(arguments: Map<String, String>) {
        this.arguments.clear()
        this.arguments.putAll(arguments)
    }

    override fun arguments(block: MutableMap<String, String>.() -> Unit) {
        this.arguments(buildMap { DuplicateCheckingMutableMap(this).apply(block) })
    }

    override fun arguments(vararg arguments: Pair<String, String>) {
        this.arguments { arguments.forEach { (key, value) -> put(key, value) } }
    }

    override fun build(): InvocationConfiguration {
        return Configuration(
            randomSource = this.randomSource,
            ruleArguments = this.arguments.toMap(),
            stackDepth = this.stackDepth,
            type = this.type
        )
    }
}

@JvmName("newBuilder")
fun InvocationConfigurationBuilder(): InvocationConfigurationBuilder = ConfigurationBuilder()

@JvmSynthetic
fun InvocationConfiguration(block: InvocationConfigurationBuilder.() -> Unit): InvocationConfiguration {
    return InvocationConfigurationBuilder().apply(block).build()
}

@JvmName("newConfiguration")
fun InvocationConfiguration(block: Consumer<in InvocationConfigurationBuilder>): InvocationConfiguration {
    return InvocationConfiguration { block.accept(this) }
}
