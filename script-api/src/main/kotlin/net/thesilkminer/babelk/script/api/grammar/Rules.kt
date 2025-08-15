package net.thesilkminer.babelk.script.api.grammar

import net.thesilkminer.babelk.script.api.grammar.internal.ConcatenationRule
import net.thesilkminer.babelk.script.api.grammar.internal.Literal
import net.thesilkminer.babelk.script.api.grammar.internal.LiteralRule
import net.thesilkminer.babelk.script.api.invoke.InvokableRule

// TODO("Evaluate placement of this class (rules package?)")
object Rules {
    fun concatenate(first: InvokableRule, second: InvokableRule, vararg more: InvokableRule): Rule {
        return ConcatenationRule(listOf(first, second) + more.toList())
    }

    fun literal(obj: Any): Rule {
        return LiteralRule(Literal(obj))
    }
}
