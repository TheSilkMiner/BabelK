package net.thesilkminer.babelk.script.host.flow.compilation

import net.thesilkminer.babelk.script.host.flow.LoadableScript
import kotlin.script.experimental.api.CompiledScript

internal class LoadableCompiledScript(private val compiledScript: CompiledScript) : LoadableScript {
}