package net.thesilkminer.babelk.api.grammar

import net.thesilkminer.babelk.api.invoke.RandomSource
import net.thesilkminer.babelk.api.invoke.RuleInvocationSequence
import net.thesilkminer.babelk.api.invoke.RuleInvocationSequenceType

interface GrammarRule {
    val name: String
    operator fun invoke(rng: RandomSource, sequenceType: RuleInvocationSequenceType, arguments: Map<String, String>): RuleInvocationSequence

    operator fun invoke(rng: RandomSource, sequenceType: RuleInvocationSequenceType, vararg arguments: Pair<String, String>): RuleInvocationSequence {
        return this.invokeWithArguments(rng, sequenceType, arguments)
    }

    operator fun invoke(rng: RandomSource, arguments: Map<String, String>): RuleInvocationSequence {
        return this.invokeDefaultSequenceType(rng, arguments)
    }

    operator fun invoke(rng: RandomSource, vararg arguments: Pair<String, String>): RuleInvocationSequence {
        return this.invokeWithArguments(rng, arguments)
    }
}
