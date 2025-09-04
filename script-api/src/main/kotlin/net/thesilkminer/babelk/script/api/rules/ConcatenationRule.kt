package net.thesilkminer.babelk.script.api.rules

import net.thesilkminer.babelk.script.api.grammar.Rule
import net.thesilkminer.babelk.script.api.invoke.BuildingContext
import net.thesilkminer.babelk.script.api.invoke.InvocationArguments
import net.thesilkminer.babelk.script.api.invoke.InvokableRule
import net.thesilkminer.babelk.script.api.invoke.RandomSource
import net.thesilkminer.babelk.script.api.invoke.RuleState
import kotlin.collections.forEach

class ConcatenationRule(private val rules: List<InvokableRule>): Rule {
    constructor(vararg rules: InvokableRule) : this(rules.toList())
    constructor(first: InvokableRule, second: InvokableRule) : this(listOf(first, second))
    constructor(first: InvokableRule) : this(listOf(first))
    constructor(): this(listOf())

    override fun append(context: BuildingContext, state: RuleState, rng: RandomSource, arguments: InvocationArguments) {
        this.rules.forEach(context::invoke)
    }

    override fun toString(): String = "Concatenation[${this.rules}]"
}
