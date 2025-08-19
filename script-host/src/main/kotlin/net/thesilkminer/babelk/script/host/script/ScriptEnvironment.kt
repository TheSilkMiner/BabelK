package net.thesilkminer.babelk.script.host.script

import net.thesilkminer.babelk.script.api.grammar.ThisGrammar
import net.thesilkminer.babelk.script.host.flow.EvaluationEnvironment
import net.thesilkminer.babelk.script.host.interop.ScriptGrammarPack
import net.thesilkminer.babelk.script.host.script.grammar.ScriptCollectionGrammarPack

internal class ScriptEnvironment(
    private val pack: ScriptCollectionGrammarPack,
    private val wrapper: ScriptCollectionGrammarPack.() -> ScriptGrammarPack
) : EvaluationEnvironment<ScriptGrammarPack, ThisGrammar> {
    internal companion object {
        fun initializeEnvironment(grammars: Collection<String>, wrapper: ScriptCollectionGrammarPack.() -> ScriptGrammarPack): ScriptEnvironment {
            val pack = ScriptCollectionGrammarPack()
            grammars.forEach(pack::prepareNewGrammar)
            return ScriptEnvironment(pack, wrapper)
        }
    }

    override fun <R> executeOnGrammar(grammarName: String, block: ThisGrammar.() -> R): R {
        return this.pack.finalizeGrammar(grammarName, block)
    }

    override fun finalize(): ScriptGrammarPack = this.wrapper(this.pack)
}
