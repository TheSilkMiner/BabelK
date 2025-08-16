@file:JvmName("GrammarInvocationHelpers")
@file:JvmSynthetic

package net.thesilkminer.babelk.api.grammar

import net.thesilkminer.babelk.api.invoke.RandomSource
import net.thesilkminer.babelk.api.invoke.RuleInvocationSequence
import net.thesilkminer.babelk.api.invoke.RuleInvocationSequenceType

private const val RULE_PATH_SEPARATOR_CHAR = ':'

internal fun GrammarPack.findRuleVia(grammar: String, rule: String): GrammarRule {
    return this[grammar][rule]
}

internal fun GrammarPack.findRuleViaPath(rulePath: String): GrammarRule {
    val (grammar, rule) = rulePath.parseRulePath()
    return this.getRule(grammar, rule)
}

internal fun GrammarRule.invokeWithArguments(rng: RandomSource, arguments: Array<out Pair<String, String>>): RuleInvocationSequence {
    return this(rng, arguments.toNonDuplicatesMap())
}

internal fun GrammarRule.invokeDefaultSequenceType(rng: RandomSource, arguments: Map<String, String>): RuleInvocationSequence {
    return this(rng, RuleInvocationSequenceType.LIGHTWEIGHT, arguments)
}

internal fun GrammarRule.invokeWithArguments(rng: RandomSource, type: RuleInvocationSequenceType, arguments: Array<out Pair<String, String>>): RuleInvocationSequence {
    return this(rng, type, arguments.toNonDuplicatesMap())
}

private fun Array<out Pair<String, String>>.toNonDuplicatesMap(): Map<String, String> {
    val map = LinkedHashMap<String, String>(this.count())
    for ((name, value) in this) {
        require(name !in map) { "Duplicate argument '$name' found: both mapped with $value and ${map[name]}" }
        map[name] = value
    }
    return map.toMap()
}

private fun String.parseRulePath(): Pair<String, String> {
    require(this.contains(RULE_PATH_SEPARATOR_CHAR)) { "$this is not a valid rule path: expected $RULE_PATH_SEPARATOR_CHAR" }
    val (grammar, rule) = this.split(RULE_PATH_SEPARATOR_CHAR, limit = 2)
    return grammar to rule
}
