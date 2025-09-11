@file:JvmName("Babelk")

package net.thesilkminer.babelk

import net.thesilkminer.babelk.api.Logger
import net.thesilkminer.babelk.api.grammar.GrammarPack
import net.thesilkminer.babelk.api.grammar.GrammarRule
import net.thesilkminer.babelk.api.invoke.InvocationConfiguration
import net.thesilkminer.babelk.api.invoke.RuleInvocationSequence
import net.thesilkminer.babelk.api.script.ScriptBundle
import net.thesilkminer.babelk.host.compile
import net.thesilkminer.babelk.host.setupHostAndThen
import net.thesilkminer.babelk.invoke.RuleInvocationSequence

private val logger = Logger {}

internal fun compileScriptsToGrammarPack(bundle: ScriptBundle): GrammarPack {
    return setupHostAndThen {
        logger.info { "Beginning compilation for script bundle containing ${bundle.count()} scripts" }
        logger.debug { "  Bundle info: $bundle" }
        bundle.compile()
    }
}

internal fun createInvocationSequence(
    rule: GrammarRule,
    configuration: InvocationConfiguration,
    invocationCallback: (InvocationConfiguration) -> Sequence<String>
): RuleInvocationSequence {
    logger.info { "Preparing invocation sequence for ${rule.name} according to provided configuration" }
    logger.debug { "  Associated configuration data: $configuration" }
    val sequence = invocationCallback(configuration)
    return RuleInvocationSequence(configuration, sequence)
}
