package net.thesilkminer.babelk.script.definition

import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "BabelK Grammar Script",
    fileExtension = "grammar.kts",
    compilationConfiguration = GrammarScriptCompilationConfiguration::class
)
abstract class GrammarScript(val name: String)
