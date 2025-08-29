@file:JvmName("HostFlow")

package net.thesilkminer.babelk.script.host.flow

import net.thesilkminer.babelk.script.host.flow.compilation.compile
import net.thesilkminer.babelk.script.host.flow.evaluation.ScriptEvaluationException
import net.thesilkminer.babelk.script.host.flow.evaluation.evaluate
import net.thesilkminer.babelk.script.host.flow.loading.load
import net.thesilkminer.babelk.script.host.interop.LoadingCallbacks
import net.thesilkminer.babelk.script.host.interop.ScriptCollection
import net.thesilkminer.babelk.script.host.interop.ScriptCompilationException
import net.thesilkminer.babelk.script.host.interop.ScriptGrammarPack

internal fun ScriptCollection.compileAndEval(
    callbacks: LoadingCallbacks,
    evalEnvironmentCreator: EvaluationEnvironmentCreator<ScriptGrammarPack, *>
): ScriptGrammarPack {
    return try {
        this.doCompileAndEval(callbacks, evalEnvironmentCreator)
    } catch (e: ScriptEvaluationException) {
        throw ScriptCompilationException(e)
    }
}

private fun ScriptCollection.doCompileAndEval(
    callbacks: LoadingCallbacks,
    evalEnvironmentCreator: EvaluationEnvironmentCreator<ScriptGrammarPack, *>
): ScriptGrammarPack {
    val loadableCollection = this.compile()
    val evaluableCollection = loadableCollection.load(callbacks)
    return evaluableCollection.evaluate(evalEnvironmentCreator)
}
