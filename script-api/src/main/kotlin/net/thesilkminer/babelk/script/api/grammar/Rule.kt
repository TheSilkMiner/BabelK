package net.thesilkminer.babelk.script.api.grammar

import net.thesilkminer.babelk.script.api.invoke.BuildingContext
import net.thesilkminer.babelk.script.api.invoke.InvocationArguments
import net.thesilkminer.babelk.script.api.invoke.RandomSource
import net.thesilkminer.babelk.script.api.invoke.RuleState

interface Rule {
    fun append(context: BuildingContext, state: RuleState, rng: RandomSource, arguments: InvocationArguments)
}
