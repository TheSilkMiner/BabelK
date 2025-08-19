package net.thesilkminer.babelk.script.host.flow.loading

import net.thesilkminer.babelk.script.host.flow.EvaluableScript
import net.thesilkminer.babelk.script.host.flow.LoadableScript
import kotlin.reflect.KClass
import kotlin.script.experimental.api.ScriptCompilationConfiguration

internal class EvaluableHookedLoadableScript(private val script: LoadableScript, private val hook: ClassLoaderHook) : EvaluableScript {
    override val sourceLocationId: String? get() = this.script.sourceLocationId
    override val compilationConfig: ScriptCompilationConfiguration get() = this.script.compilationConfig
    override val grammarName: String get() = this.script.grammarName
    override fun loadMainClass(): KClass<*> = this.hook(this.grammarName, this.script.mainClassName)
}
