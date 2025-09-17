@file:JvmName("GrammarClassNameHandler")

package net.thesilkminer.babelk.script.host.flow

import net.thesilkminer.babelk.script.host.interop.Script

private const val SCRIPT_PREFIX = "babelk.scripting.grammars"

private val grammarNamePattern = Regex("[a-zA-Z0-9][a-zA-Z0-9_/ ]*")

internal fun String.toGrammarClassName(): String {
    val packageName = this.replace('/', '.')
    val grammarName = packageName.substringAfterLast('.')
    return "$SCRIPT_PREFIX.$packageName.$grammarName"
}

internal fun String.nameFromGrammarClass(): String {
    require(this.startsWith(SCRIPT_PREFIX)) { "Invalid class name $this" }
    return this.removePrefix("$SCRIPT_PREFIX.").substringBeforeLast('.').replace('.', '/')
}

internal fun Script.extractGrammarNameFromScriptNameOrNull(): String? {
    return runCatching { this.name.removeScriptSuffix().also { it.verifyValidGrammarName() } }.getOrNull()
}

internal fun String.verifyValidGrammarName() {
    require(this.isValidGrammarName) { "Grammar name must match pattern $grammarNamePattern, but got $this" }
}

internal val String.isValidGrammarName: Boolean get() = this matches grammarNamePattern

private fun String.removeScriptSuffix(): String = this.removeSuffix(".grammar.kts")
