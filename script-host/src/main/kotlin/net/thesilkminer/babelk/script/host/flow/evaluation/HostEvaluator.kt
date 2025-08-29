@file:JvmName("HostEvaluator")

package net.thesilkminer.babelk.script.host.flow.evaluation

import net.thesilkminer.babelk.script.api.grammar.ThisGrammar
import net.thesilkminer.babelk.script.definition.GrammarScript
import net.thesilkminer.babelk.script.host.Log
import net.thesilkminer.babelk.script.host.flow.EvaluableScript
import net.thesilkminer.babelk.script.host.flow.EvaluationEnvironment
import net.thesilkminer.babelk.script.host.flow.EvaluationEnvironmentCreator
import net.thesilkminer.babelk.script.host.flow.reportDiagnostics
import net.thesilkminer.babelk.script.host.flow.rethrowOnError
import net.thesilkminer.babelk.script.host.interop.ScriptGrammarPack
import net.thesilkminer.babelk.script.host.withinCoroutine
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.api.scriptExecutionWrapper
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

private val log = Log {}

internal fun Collection<EvaluableScript>.evaluate(environmentCreator: EvaluationEnvironmentCreator<ScriptGrammarPack, *>): ScriptGrammarPack {
    log.info { this.joinToString(prefix = "Evaluating ${this.count()} scripts, corresponding to grammars named [", postfix = "]") { it.grammarName } }

    val environment = environmentCreator(this)
    val results = this.map { it.evaluate(environment) }

    results.verifyResultsAndRethrow()

    log.info { "Finalizing generated grammar pack" }
    return environment.finalize()
}

private operator fun <T, G : ThisGrammar> EvaluationEnvironmentCreator<T, G>.invoke(collection: Collection<EvaluableScript>): EvaluationEnvironment<T, G> {
    return this(collection.map(EvaluableScript::grammarName))
}

private fun EvaluableScript.evaluate(environment: EvaluationEnvironment<*, *>): Result<Unit> {
    return this.evaluateWithResult(environment)
        .reportDiagnostics(log)
        .rethrowOnError(log) { "An error occurred while attempting to evaluate script for grammar ${this.grammarName}: please refer to the log output" }
        .handleReturnValue()
}

private fun EvaluableScript.evaluateWithResult(environment: EvaluationEnvironment<*, *>): ResultWithDiagnostics<EvaluationResult> {
    val grammarName = this.grammarName
    return environment.executeOnGrammar(grammarName) { this@evaluateWithResult.evaluateWithResultAndGrammar(grammarName) }
}

context(_: ThisGrammar)
private fun EvaluableScript.evaluateWithResultAndGrammar(name: String): ResultWithDiagnostics<EvaluationResult> {
    val evaluator = BasicJvmScriptEvaluator()
    val compiledScript = CompiledEvaluableScript(this)
    val evaluationConfiguration = createJvmEvaluationConfigurationFromTemplate<GrammarScript> {
        constructorArgs(grammarName)
        implicitReceivers(contextOf<ThisGrammar>())
        scriptExecutionWrapper<Any?> {
            log.info { "Evaluating script $name" }
            it()
        }
    }
    return withinCoroutine { evaluator(compiledScript, evaluationConfiguration) }
}

context(_: EvaluableScript)
private fun EvaluationResult.handleReturnValue(): Result<Unit> {
    return this.returnValue.handleValue()
}

context(script: EvaluableScript)
private fun ResultValue.handleValue(): Result<Unit> {
    return when (this) {
        is ResultValue.Unit -> Result.success(Unit)
        is ResultValue.Error -> Result.failure(ScriptEvaluationException(script.grammarName, this.error))
        is ResultValue.Value -> error("Grammar scripts should not evaluate to a value")
        ResultValue.NotEvaluated -> error("Script was not evaluated")
    }
}

private fun Collection<Result<Unit>>.verifyResultsAndRethrow() {
    this.mapNotNull(Result<*>::exceptionOrNull)
        .reduceOrNull { original, additional -> original.also { it.addSuppressed(additional) } }
        ?.let { throw it }
}
