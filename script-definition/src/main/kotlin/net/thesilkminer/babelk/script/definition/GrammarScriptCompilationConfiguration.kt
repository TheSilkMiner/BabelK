package net.thesilkminer.babelk.script.definition

import net.thesilkminer.babelk.script.api.grammar.ThisGrammar
import kotlin.reflect.typeOf
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.implicitReceivers

internal object GrammarScriptCompilationConfiguration : ScriptCompilationConfiguration({
    implicitReceivers(typeOf<ThisGrammar>())
    defaultImports(
        "net.thesilkminer.babelk.script.api.*",
        "net.thesilkminer.babelk.script.api.collection.*",
        "net.thesilkminer.babelk.script.api.grammar.*",
        "net.thesilkminer.babelk.script.api.invoke.*",
        "net.thesilkminer.babelk.script.api.provider.*",
        "net.thesilkminer.babelk.script.dsl.*",
    )
}) {
    @Suppress("unused") private fun readResolve(): Any = GrammarScriptCompilationConfiguration
}
