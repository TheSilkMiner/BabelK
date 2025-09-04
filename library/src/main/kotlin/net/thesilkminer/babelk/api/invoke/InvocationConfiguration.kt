package net.thesilkminer.babelk.api.invoke

import net.thesilkminer.babelk.api.grammar.GrammarRule

interface InvocationConfiguration {
    interface SequenceConfiguration {
        val type: RuleInvocationSequenceType
        val stackDepth: Int
        val onStackOverflow: (GrammarRule, StackOverflowError?) -> CharSequence?
    }

    val randomSource: RandomSource
    val ruleArguments: Map<String, String>
    val sequenceConfiguration: SequenceConfiguration
}
