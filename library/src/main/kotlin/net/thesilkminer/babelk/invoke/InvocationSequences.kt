@file:JvmName("InvocationSequences")

package net.thesilkminer.babelk.invoke

import net.thesilkminer.babelk.api.invoke.InvocationConfiguration
import net.thesilkminer.babelk.api.invoke.RuleInvocation
import net.thesilkminer.babelk.api.invoke.RuleInvocationSequence
import net.thesilkminer.babelk.api.invoke.RuleInvocationSequenceType
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.concurrent.withLock
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private class InvocationStorage(private val onMissingEntry: (requested: Long, maxAvailable: Long) -> RuleInvocation) {
    private val backend = mutableListOf<RuleInvocation>()

    // We do not use a read-write lock because we assume most use cases will actually involve writing, not simply reading
    // already written data. We might change this in the future if needed
    private val lock = ReentrantLock()

    operator fun get(index: Long): RuleInvocation {
        val narrowedIndex = index.narrowIndex()
        this.lock.withLock { this.backend.getOrNull(narrowedIndex) }?.let { return it }

        return this.lock.withLock {
            // Check again in case some other thread has been competing for the lock at this point, won, and wrote the
            // value we are interested in already
            this.backend.getOrNull(narrowedIndex)?.let { return it }

            val maxAvailable = this.backend.count() - 1 // Matches this.backend.last().sequenceNumber
            val toAdd = this.onMissingEntry(index, maxAvailable.toLong())
            this[index] = toAdd
            toAdd
        }
    }

    operator fun set(index: Long, value: RuleInvocation) {
        require(value.sequenceNumber == index) { "Index to set the value to must be the same as the sequence number" }
        val position = index.narrowIndex()
        this.lock.withLock {
            val count = this.backend.count()

            when {
                position < count -> {
                    val elementAt = this.backend[position]
                    if (elementAt != value) {
                        error("Invariant broken: attempted to change the past somehow? $elementAt is not the same as $value")
                    }
                }
                position == count -> this.backend.add(position, value)
                else -> TODO("Randomized insertion is not yet supported")
            }
        }
    }

    private fun Long.narrowIndex(): Int {
        return when {
            this < 0 -> throw IndexOutOfBoundsException("Index must be positive")
            this > Int.MAX_VALUE -> TODO("Support for more than ${Int.MAX_VALUE} entries is not yet available")
            else -> this.toInt()
        }
    }
}

private data class InvocationResult(override val sequenceNumber: Long, override val result: String) : RuleInvocation

private sealed class CallbackBasedInvocationSequence(
    final override val type: RuleInvocationSequenceType,
    private val callback: () -> RuleInvocation
) : RuleInvocationSequence {
    final override fun next(): RuleInvocation = this.callback().also(this::onNext)
    protected abstract fun onNext(invocation: RuleInvocation)

    final override fun toString(): String = "RuleInvocationSequence of type ${this.type}"
}

private sealed class MemorizingInvocationSequence(
    type: RuleInvocationSequenceType,
    callback: () -> RuleInvocation
) : CallbackBasedInvocationSequence(type, callback) {
    private val storage = InvocationStorage(this::onMissingEntry)

    final override fun onNext(invocation: RuleInvocation) {
        this.storage[invocation.sequenceNumber] = invocation
    }

    final override fun nth(index: Int): RuleInvocation {
        return this.storage[index.toLong()]
    }

    protected abstract fun onMissingEntry(requested: Long, maxAvailable: Long): RuleInvocation
}

private class NonStoringInvocationSequence(
    type: RuleInvocationSequenceType,
    callback: () -> RuleInvocation
) : CallbackBasedInvocationSequence(type, callback) {
    override fun onNext(invocation: RuleInvocation) = Unit
    override fun nth(index: Int): Nothing = throw UnsupportedOperationException("nth operation not supported in a sequence of type ${this.type}")
}

private class NonCreatingMemorizingInvocationSequence(
    type: RuleInvocationSequenceType,
    callback: () -> RuleInvocation,
    private val errorMessage: String
) : MemorizingInvocationSequence(type, callback) {
    override fun onMissingEntry(requested: Long, maxAvailable: Long): Nothing = error(this.errorMessage)
}

private class NextInvokingMemorizingInvocationSequence(
    type: RuleInvocationSequenceType,
    callback: () -> RuleInvocation
) : MemorizingInvocationSequence(type, callback) {
    private class ThreadLocalVariableDelegate<T>(private val local: ThreadLocal<T>) : ReadWriteProperty<Any?, T> {
        constructor(initial: T) : this(ThreadLocal.withInitial { initial })

        override fun getValue(thisRef: Any?, property: KProperty<*>): T = this.local.get()
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = this.local.set(value)
    }

    private var reentranceCheck by ThreadLocalVariableDelegate(false)

    override fun onMissingEntry(requested: Long, maxAvailable: Long): RuleInvocation {
        if (this.reentranceCheck) error("Reentrancy detected: this is not supported")
        this.reentranceCheck = true
        try {
            val spuriousNextCalls = requested - maxAvailable - 1

            var counter = spuriousNextCalls
            while (counter > 0) {
                val quantity = (counter % Int.MAX_VALUE).toInt()
                counter -= quantity
                repeat(quantity) { this.next() } // We do not use this.next(quantity) to avoid list allocation
            }

            return this.next()
        } finally {
            this.reentranceCheck = false
        }
    }
}

private const val RETAINING_SEQUENCE_ILLEGAL_N = "Retaining rule invocation sequence does not support accessing values that have not yet been computed"

internal fun RuleInvocationSequence(configuration: InvocationConfiguration, sequence: Sequence<String>): RuleInvocationSequence {
    val callback = sequence.toNextCallback()
    return when (val type = configuration.sequenceConfiguration.type) {
        RuleInvocationSequenceType.LIGHTWEIGHT -> NonStoringInvocationSequence(type, callback)
        RuleInvocationSequenceType.RETAINING -> NonCreatingMemorizingInvocationSequence(type, callback, RETAINING_SEQUENCE_ILLEGAL_N)
        RuleInvocationSequenceType.RANDOM_ACCESS -> NextInvokingMemorizingInvocationSequence(type, callback)
    }
}

@OptIn(ExperimentalAtomicApi::class)
private fun Sequence<String>.toNextCallback(): () -> RuleInvocation {
    val iterator = this.constrainOnce().iterator()
    val counter = AtomicLong(0L)
    return {
        val result = iterator.next()
        val sequenceNumber = counter.fetchAndIncrement()
        InvocationResult(sequenceNumber, result)
    }
}
