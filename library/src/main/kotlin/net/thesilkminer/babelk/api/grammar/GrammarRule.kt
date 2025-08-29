package net.thesilkminer.babelk.api.grammar

import net.thesilkminer.babelk.api.invoke.InvocationConfiguration
import net.thesilkminer.babelk.api.invoke.InvocationConfigurationDsl
import net.thesilkminer.babelk.api.invoke.RuleInvocationSequence
import java.util.function.Consumer

interface GrammarRule {
    val name: String
    operator fun invoke(configuration: InvocationConfiguration): RuleInvocationSequence

    @JvmSynthetic
    operator fun invoke(builderConfiguration: InvocationConfigurationDsl.() -> Unit): RuleInvocationSequence {
        return this.invokeCreatingConfiguration(builderConfiguration)
    }

    operator fun invoke(builderConfiguration: Consumer<in InvocationConfigurationDsl>): RuleInvocationSequence {
        return this.invokeCreatingConfigurationForJava(builderConfiguration)
    }

    operator fun invoke(): RuleInvocationSequence {
        return this.invokeWithDefaults()
    }
}
