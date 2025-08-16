package net.thesilkminer.babelk.api.grammar

interface Grammar {
    val name: String
    val rules: Collection<GrammarRule>
    fun findRule(name: String): GrammarRule?

    fun getRule(name: String): GrammarRule = this.findRule(name) ?: error("No rule named '$name' found in grammar '${this.name}'")
    operator fun get(name: String): GrammarRule = this.getRule(name)
}
