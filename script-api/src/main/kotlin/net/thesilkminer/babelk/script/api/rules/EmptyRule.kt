package net.thesilkminer.babelk.script.api.rules

import net.thesilkminer.babelk.script.api.grammar.Rule
import net.thesilkminer.babelk.script.api.invoke.BuildingContext
import net.thesilkminer.babelk.script.api.invoke.InvocationArguments
import net.thesilkminer.babelk.script.api.invoke.RandomSource
import net.thesilkminer.babelk.script.api.invoke.RuleState

object EmptyRule : Rule {
    override fun append(context: BuildingContext, state: RuleState, rng: RandomSource, arguments: InvocationArguments) = Unit
    override fun toString(): String = "Empty"
}
