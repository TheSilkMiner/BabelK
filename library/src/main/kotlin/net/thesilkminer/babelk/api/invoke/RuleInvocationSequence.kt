package net.thesilkminer.babelk.api.invoke

import java.util.stream.Stream
import kotlin.streams.asStream

interface RuleInvocationSequence : Sequence<RuleInvocation>, Iterable<RuleInvocation> {
    val type: RuleInvocationSequenceType

    fun next(): RuleInvocation
    fun next(quantity: Int): List<RuleInvocation> = List(quantity) { this.next() }

    fun nth(index: Int): RuleInvocation

    override operator fun iterator(): Iterator<RuleInvocation> = iterator { yield(this@RuleInvocationSequence.next()) }

    fun asIterable(): Iterable<RuleInvocation> = this
    fun asIterator(): Iterator<RuleInvocation> = this.iterator()
    fun asSequence(): Sequence<RuleInvocation> = this
    fun asStream(): Stream<RuleInvocation> = this.asSequence().asStream()

    fun nextResult(): String = this.next().result
    fun nextResult(quantity: Int): List<String> = this.next(quantity).map(RuleInvocation::result)
    fun nthResult(index: Int): String = this.nth(index).result
}
