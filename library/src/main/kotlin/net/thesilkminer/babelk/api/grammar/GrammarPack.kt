package net.thesilkminer.babelk.api.grammar

interface GrammarPack {
    val grammars: Collection<Grammar>
    fun findGrammar(name: String): Grammar?

    fun getGrammar(name: String): Grammar = this.findGrammar(name) ?: error("No grammar with name '$name' found in the current pack")
    operator fun get(name: String): Grammar = this.getGrammar(name)

    fun getRule(grammar: String, rule: String): GrammarRule = this.findRuleVia(grammar, rule)
    fun getRule(rulePath: String): GrammarRule = this.findRuleViaPath(rulePath)
    operator fun get(grammar: String, rule: String): GrammarRule = this.getRule(grammar, rule)
}
