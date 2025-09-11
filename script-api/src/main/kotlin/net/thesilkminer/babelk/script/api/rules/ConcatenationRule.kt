package net.thesilkminer.babelk.script.api.rules

import net.thesilkminer.babelk.script.api.grammar.Rule
import net.thesilkminer.babelk.script.api.invoke.BuildingContext
import net.thesilkminer.babelk.script.api.invoke.InvocationArguments
import net.thesilkminer.babelk.script.api.invoke.InvokableRule
import net.thesilkminer.babelk.script.api.invoke.RandomSource
import net.thesilkminer.babelk.script.api.invoke.RuleState
import kotlin.collections.forEach

class ConcatenationRule(private val rules: List<InvokableRule>, private val separator: InvokableRule? = null): Rule {
    constructor(vararg rules: InvokableRule, separator: InvokableRule? = null) : this(rules.toList(), separator)
    constructor(first: InvokableRule, second: InvokableRule, separator: InvokableRule? = null) : this(listOf(first, second), separator)
    constructor(first: InvokableRule) : this(listOf(first))
    constructor(): this(listOf())

    override fun append(context: BuildingContext, state: RuleState, rng: RandomSource, arguments: InvocationArguments) {
        var isFirst = true
        this.rules.forEach {
            if (!isFirst && this.separator != null) {
                context.invoke(this.separator)
            }
            isFirst = false
            context.invoke(it)
        }
    }

    override fun toString(): String = "Concatenation{${this.separator}}[${this.rules}]"
}
