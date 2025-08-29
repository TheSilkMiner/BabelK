@file:JvmName("GrammarInvocationHelpers")
@file:JvmSynthetic

package net.thesilkminer.babelk.api.grammar

import net.thesilkminer.babelk.api.invoke.InvocationConfiguration
import net.thesilkminer.babelk.api.invoke.InvocationConfigurationDsl
import net.thesilkminer.babelk.api.invoke.RuleInvocationSequence
import java.util.function.Consumer

private const val RULE_PATH_SEPARATOR_CHAR = ':'

internal fun GrammarPack.findRuleVia(grammar: String, rule: String): GrammarRule {
    return this[grammar][rule]
}

internal fun GrammarPack.findRuleViaPath(rulePath: String): GrammarRule {
    val (grammar, rule) = rulePath.parseRulePath()
    return this.getRule(grammar, rule)
}

private fun String.parseRulePath(): Pair<String, String> {
    require(this.contains(RULE_PATH_SEPARATOR_CHAR)) { "$this is not a valid rule path: expected $RULE_PATH_SEPARATOR_CHAR" }
    val (grammar, rule) = this.split(RULE_PATH_SEPARATOR_CHAR, limit = 2)
    return grammar to rule
}

internal fun GrammarRule.invokeCreatingConfiguration(block: InvocationConfigurationDsl.() -> Unit): RuleInvocationSequence {
    return this(InvocationConfiguration(block))
}

internal fun GrammarRule.invokeCreatingConfigurationForJava(consumer: Consumer<in InvocationConfigurationDsl>): RuleInvocationSequence {
    return this(InvocationConfiguration(consumer))
}

internal fun GrammarRule.invokeWithDefaults(): RuleInvocationSequence {
    return this {}
}
