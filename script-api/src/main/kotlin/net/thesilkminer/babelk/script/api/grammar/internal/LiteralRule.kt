package net.thesilkminer.babelk.script.api.grammar.internal

import net.thesilkminer.babelk.script.api.grammar.Rule
import net.thesilkminer.babelk.script.api.invoke.BuildingContext
import net.thesilkminer.babelk.script.api.invoke.InvocationArguments
import net.thesilkminer.babelk.script.api.invoke.RandomSource
import net.thesilkminer.babelk.script.api.invoke.RuleState

internal class LiteralRule(private val literal: Literal) : Rule {
    override fun append(context: BuildingContext, state: RuleState, rng: RandomSource, arguments: InvocationArguments) {
        this.literal.appendLiteral(context)
    }

    override fun toString(): String = "LiteralRule[${this.literal}]"
}
