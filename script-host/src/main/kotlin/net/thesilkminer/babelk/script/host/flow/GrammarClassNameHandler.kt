@file:JvmName("GrammarClassNameHandler")

package net.thesilkminer.babelk.script.host.flow

import net.thesilkminer.babelk.script.host.interop.Script

private const val SCRIPT_PREFIX = "babelk.scripting.grammars"

private val grammarNamePattern = Regex("[a-zA-Z0-9][a-zA-Z0-9_/ ]*")

internal fun String.toGrammarClassName(): String = "$SCRIPT_PREFIX.$this.$this"

internal fun String.nameFromGrammarClass(): String {
    require(this.startsWith(SCRIPT_PREFIX)) { "Invalid class name $this" }
    return this.removePrefix("$SCRIPT_PREFIX.").substringBefore('.')
}

internal fun Script.extractGrammarNameFromScriptNameOrNull(): String? {
    return runCatching { this.name.also { it.verifyValidGrammarName() } }.getOrNull()
}

internal fun String.verifyValidGrammarName() {
    require(this.isValidGrammarName) { "Grammar name must match pattern $grammarNamePattern, but got $this" }
}

internal val String.isValidGrammarName: Boolean get() = this matches grammarNamePattern
