@file:JvmName("EvalEnvironment")

package net.thesilkminer.babelk.script.host.flow

import net.thesilkminer.babelk.script.api.grammar.ThisGrammar

typealias EvaluationEnvironmentCreator<T, G> = (grammarNames: Collection<String>) -> EvaluationEnvironment<T, G>

interface EvaluationEnvironment<out T, out G : ThisGrammar> {
    fun <R> executeOnGrammar(grammarName: String, block: G.() -> R): R
    fun finalize(): T
}
