package net.thesilkminer.babelk.script.host.script.collection

import net.thesilkminer.babelk.script.api.grammar.NamedRule
import net.thesilkminer.babelk.script.api.grammar.Rule
import net.thesilkminer.babelk.script.api.grammar.RuleBuilderContext
import net.thesilkminer.babelk.script.api.invoke.BuildingContext
import net.thesilkminer.babelk.script.api.invoke.InvocationArguments
import net.thesilkminer.babelk.script.api.invoke.RandomSource
import net.thesilkminer.babelk.script.api.invoke.RuleState
import net.thesilkminer.babelk.script.api.provider.NamedObjectProvider

internal class GrammarRuleNamedObjectCollection : MapBackedMutableNamedObjectCollection<NamedRule, Rule, RuleBuilderContext>() {
    private class SimpleNamedRule(override val name: String, private val rule: Rule) : NamedRule {
        override fun append(context: BuildingContext, state: RuleState, rng: RandomSource, arguments: InvocationArguments) {
            // We are not going to add one more indirection layer as we are merely wrapping the rule to make it named
            // The caller (and thus BuildingContext) will be responsible for setting up the proper RuleState, so we
            // can simply forward it.
            return this.rule.append(context, state, rng, arguments)
        }

        override fun toString(): String = "${this.name} -> ${this.rule}"
    }

    private class DelayedLookupRule(override val name: String, lookup: () -> NamedRule) : NamedRule {
        private val delegate by lazy(lookup)

        override fun append(context: BuildingContext, state: RuleState, rng: RandomSource, arguments: InvocationArguments) {
            return this.delegate.append(context, state, rng, arguments)
        }

        override fun toString(): String = "${this.name} -> delayed lookup"
    }

    private object Context : RuleBuilderContext

    override fun contextForObject(name: String): RuleBuilderContext {
        return Context
    }

    override fun Rule.toNamedObject(name: String): NamedRule {
        return SimpleNamedRule(name, this)
    }

    override fun providerForObject(name: String, obj: NamedRule): NamedObjectProvider<NamedRule> {
        return SimpleNamedObjectProvider(name, obj)
    }

    override fun providerForLookup(name: String, lookup: () -> NamedRule): NamedObjectProvider<NamedRule> {
        return this.providerForObject(name, DelayedLookupRule(name, lookup))
    }
}
