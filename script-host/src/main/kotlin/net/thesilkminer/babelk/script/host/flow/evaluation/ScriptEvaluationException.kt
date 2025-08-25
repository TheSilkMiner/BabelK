package net.thesilkminer.babelk.script.host.flow.evaluation

internal class ScriptEvaluationException(
    scriptName: String,
    cause: Throwable
) : RuntimeException("An error occurred while trying to evaluate script $scriptName", cause)
