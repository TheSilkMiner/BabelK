@file:JvmName("Babelk")

package net.thesilkminer.babelk

import net.thesilkminer.babelk.api.Logger
import net.thesilkminer.babelk.api.grammar.GrammarPack
import net.thesilkminer.babelk.api.script.ScriptBundle
import net.thesilkminer.babelk.host.compile
import net.thesilkminer.babelk.host.setupHostAndThen

private val logger = Logger {}

internal fun compileScriptsToGrammarPack(bundle: ScriptBundle): GrammarPack {
    return setupHostAndThen {
        logger.info { "Beginning compilation for script bundle containing ${bundle.count()} scripts" }
        logger.debug { "  Bundle info: $bundle" }
        bundle.compile()
    }
}
