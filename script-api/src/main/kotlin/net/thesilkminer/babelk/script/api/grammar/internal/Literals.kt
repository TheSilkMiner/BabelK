@file:JvmName("Literals")

package net.thesilkminer.babelk.script.api.grammar.internal

import net.thesilkminer.babelk.script.api.grammar.Rule
import net.thesilkminer.babelk.script.api.invoke.BuildingContext
import net.thesilkminer.babelk.script.api.invoke.InvokableRule
import net.thesilkminer.babelk.script.api.provider.Provider
import net.thesilkminer.babelk.script.api.provider.nameOrNull

internal sealed interface Literal {
    fun appendLiteral(context: BuildingContext)
}

private sealed class PrimitiveLiteral : Literal {
    abstract val content: String

    final override fun appendLiteral(context: BuildingContext) {
        context.append(this.content)
    }

    final override fun toString(): String = "Literal[${this.content}]"
}

private class StringLiteral(override val content: String) : PrimitiveLiteral()

private class CharSequenceLiteral(private val sequence: CharSequence) : PrimitiveLiteral() {
    override val content: String get() = this.sequence.toString()
}

private class CharacterLiteral(private val char: Char) : PrimitiveLiteral() {
    override val content: String get() = this.char.toString()
}

private class NumberLiteral(private val number: Number) : PrimitiveLiteral() {
    override val content: String get() = this.number.toString()
}

private class InvokableRuleLiteral(private val invokableRule: InvokableRule) : Literal {
    override fun appendLiteral(context: BuildingContext) {
        context.invoke(this.invokableRule)
    }

    override fun toString(): String = "Literal[${this.invokableRule}]"
}

private class ProviderLiteral(private val provider: Provider<*>) : Literal {
    override fun appendLiteral(context: BuildingContext) {
        Literal(provider.get()).appendLiteral(context)
    }

    override fun toString(): String = "Literal[Provider of ${this.provider.nameOrNull ?: "an anonymous object"}]"
}

private class RawLiteral(private val obj: Any?) : Literal {
    override fun appendLiteral(context: BuildingContext) {
        context.append(this.obj.toString())
    }

    override fun toString(): String = this.obj?.let { "${it::class.qualifiedName} -> $it" }.toString()
}

internal fun Literal(obj: Any?): Literal {
    return when (obj) {
        is String -> StringLiteral(obj)
        is CharSequence -> CharSequenceLiteral(obj)
        is Char -> CharacterLiteral(obj)
        is Number -> NumberLiteral(obj)
        is InvokableRule -> InvokableRuleLiteral(obj)
        is Rule -> InvokableRuleLiteral(InvokableRule(Provider { obj }))
        is Provider<*> -> ProviderLiteral(obj)
        else -> RawLiteral(obj)
    }
}
