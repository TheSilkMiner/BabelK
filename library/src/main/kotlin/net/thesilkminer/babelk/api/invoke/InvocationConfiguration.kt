package net.thesilkminer.babelk.api.invoke

interface InvocationConfiguration {
    interface SequenceConfiguration {
        val type: RuleInvocationSequenceType
        val stackDepth: Int
    }

    val randomSource: RandomSource
    val ruleArguments: Map<String, String>
    val sequenceConfiguration: SequenceConfiguration
}
