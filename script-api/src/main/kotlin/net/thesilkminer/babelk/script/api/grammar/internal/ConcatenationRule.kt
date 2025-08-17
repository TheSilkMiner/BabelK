package net.thesilkminer.babelk.script.api.grammar.internal

import net.thesilkminer.babelk.script.api.grammar.Rule
import net.thesilkminer.babelk.script.api.invoke.BuildingContext
import net.thesilkminer.babelk.script.api.invoke.InvocationArguments
import net.thesilkminer.babelk.script.api.invoke.InvokableRule
import net.thesilkminer.babelk.script.api.invoke.RandomSource
import net.thesilkminer.babelk.script.api.invoke.RuleState

internal class ConcatenationRule(private val rules: List<InvokableRule>): Rule {
    override fun append(context: BuildingContext, state: RuleState, rng: RandomSource, arguments: InvocationArguments) {
        this.rules.forEach(context::invoke)
    }

    override fun toString(): String = "Concatenation[${this.rules}]"
}