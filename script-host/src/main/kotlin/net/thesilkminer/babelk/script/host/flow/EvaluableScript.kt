package net.thesilkminer.babelk.script.host.flow

import kotlin.reflect.KClass
import kotlin.script.experimental.api.ScriptCompilationConfiguration

interface EvaluableScript {
    val sourceLocationId: String?
    val compilationConfig: ScriptCompilationConfiguration
    val grammarName: String
    fun loadMainClass(): KClass<*>
}
