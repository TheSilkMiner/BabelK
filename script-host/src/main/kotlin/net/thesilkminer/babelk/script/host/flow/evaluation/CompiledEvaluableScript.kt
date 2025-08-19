package net.thesilkminer.babelk.script.host.flow.evaluation

import net.thesilkminer.babelk.script.host.flow.EvaluableScript
import kotlin.reflect.KClass
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.makeFailureResult

internal class CompiledEvaluableScript(private val evaluableScript: EvaluableScript) : CompiledScript {
    override val sourceLocationId: String? get() = this.evaluableScript.sourceLocationId ?: super.sourceLocationId
    override val compilationConfiguration: ScriptCompilationConfiguration get() = this.evaluableScript.compilationConfig

    override suspend fun getClass(scriptEvaluationConfiguration: ScriptEvaluationConfiguration?): ResultWithDiagnostics<KClass<*>> {
        return runCatching { this.evaluableScript.loadMainClass().asSuccess() }
            .recover { createLoadingError(it) }
            .getOrThrow()
    }

    private fun createLoadingError(error: Throwable): ResultWithDiagnostics<KClass<*>> {
        return makeFailureResult(
            ScriptDiagnostic(
                ScriptDiagnostic.unspecifiedError,
                "Unable to instantiate script main class for ${this.evaluableScript.grammarName}",
                sourcePath = this.sourceLocationId,
                exception = error
            )
        )
    }
}
