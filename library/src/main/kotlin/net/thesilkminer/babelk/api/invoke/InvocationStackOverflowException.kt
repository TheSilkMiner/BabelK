package net.thesilkminer.babelk.api.invoke

import net.thesilkminer.babelk.api.grammar.GrammarRule

class InvocationStackOverflowException : RuntimeException {
    val grammarRule: GrammarRule

    constructor(grammarRule: GrammarRule, message: String) : super(message) {
        this.grammarRule = grammarRule
    }

    constructor(grammarRule: GrammarRule, cause: Throwable?) : super(cause) {
        this.grammarRule = grammarRule
    }

    constructor(grammarRule: GrammarRule, message: String, cause: Throwable?) : super(message, cause) {
        this.grammarRule = grammarRule
    }
}