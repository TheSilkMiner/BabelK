package net.thesilkminer.babelk.script.api.invoke

import net.thesilkminer.babelk.script.api.grammar.Rule
import net.thesilkminer.babelk.script.api.provider.Provider
import net.thesilkminer.babelk.script.api.provider.nameOrNull

class InvokableRule(val rule: Provider<Rule>, val arguments: InvocationArguments = InvocationArguments()) {
    val name: String get() = this.rule.nameOrNull ?: "<anonymous>"
    override fun toString(): String = "${this.name}${this.arguments}"
}
