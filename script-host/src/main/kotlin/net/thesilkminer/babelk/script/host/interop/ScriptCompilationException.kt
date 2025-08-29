package net.thesilkminer.babelk.script.host.interop

import net.thesilkminer.babelk.script.host.flow.evaluation.ScriptEvaluationException

class ScriptCompilationException internal constructor(
    cause: ScriptEvaluationException
) : Exception(cause.message ?: "An error occurred while attempting to compile a script to a grammar pack", cause)
