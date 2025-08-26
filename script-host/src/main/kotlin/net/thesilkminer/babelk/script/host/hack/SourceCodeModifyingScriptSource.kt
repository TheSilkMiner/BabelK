@file:JvmName("SourceCodeModifyingScriptSource")

package net.thesilkminer.babelk.script.host.hack

import net.thesilkminer.babelk.script.api.GrammarName
import net.thesilkminer.babelk.script.host.flow.extractGrammarNameFromScriptNameOrNull
import net.thesilkminer.babelk.script.host.flow.toGrammarClassName
import net.thesilkminer.babelk.script.host.flow.verifyValidGrammarName
import net.thesilkminer.babelk.script.host.interop.Script
import java.io.Serializable
import java.net.URL
import java.nio.file.Path
import kotlin.script.experimental.api.ExternalSourceCode

private class SourceCodeModifyingScriptSourceCode(
    private val delegate: ExternalSourceCode,
    private val nameOverride: String?,
    private val modifiedContents: () -> String
) : ExternalSourceCode, Serializable {
    override val externalLocation: URL get() = this.delegate.externalLocation
    override val locationId: String? get() = this.delegate.locationId
    override val name: String? get() = this.nameOverride ?: this.delegate.name
    override val text: String get() = this.modifiedContents()

    override fun toString(): String = "${this.delegate} (modified with ${this.nameOverride?.let { "new name $it and " } ?: ""}new text)"
}

private class FakeScriptMerelyForReusingMethodsInAnotherContext(private val delegate: ExternalSourceCode) : Script {
    override val name: String get() = this.delegate.name ?: error("Name missing: this is impossible")
    override val path: Path? get() = null
    override val content: String get() = ""
}

internal fun ExternalSourceCode.setUpSourceCodeEditingToWorkAroundKtsCompilerBugs(): ExternalSourceCode {
    requireNotNull(this.name) { "Invokers of this method must ensure the source code has a valid name" }

    val contentLines = this.text.splitBackToLines()
    val (grammarName, annotationPosition) = this.extractGrammarName(contentLines)

    // Note that a file name override also replaces the SourceCode attribute on the generated class
    // TODO("Figure out a way to keep the original file name just in case")
    val nameOverride = grammarName.computeFileNameOverride()
    val targetPackage = nameOverride.removeSuffix(".kts").toGrammarClassName().substringBeforeLast('.')
    val packageLine = annotationPosition + 1

    val replacesLine = contentLines.getOrNull(packageLine).isNullOrBlank()
    val nextStep = packageLine + (if (replacesLine) 1 else 0)

    val contentSequence = sequence {
        yieldAll(contentLines.subList(0, packageLine))
        yield("package $targetPackage;")
        yieldAll(contentLines.subList(nextStep, contentLines.count()))
    }

    return SourceCodeModifyingScriptSourceCode(this, nameOverride, contentSequence::mergeBackIntoString)
}

private fun String.splitBackToLines(): List<String> {
    return this.split(Regex("\\r\\n?"))
}

private fun ExternalSourceCode.extractGrammarName(content: List<String>): Pair<String, Int> {
    val suggestedName = FakeScriptMerelyForReusingMethodsInAnotherContext(this).extractGrammarNameFromScriptNameOrNull()
    val (proposedName, annotationPosition) = content.scanForGrammarAnnotation() ?: (null to -1)
    proposedName?.verifyValidGrammarName()

    val grammarName = proposedName ?: suggestedName
    requireNotNull(grammarName) { "File name ${this.name} cannot be used as grammar name and no alternative name was provided" }

    return grammarName to annotationPosition
}

// 0 -> @ found
// 1 -> file found
// 2 -> : found
// 3 -> GrammarName found
// 4 -> ( found
// 5 -> string literal found => use
// 6 -> found package or import or anything else
// 7 -> comment begin
private typealias ScanState = Int

private val grammarNameClassSimpleName = GrammarName::class.simpleName!!
private val grammarNameClassQualifiedName = GrammarName::class.qualifiedName!!

private fun List<String>.scanForGrammarAnnotation(): Pair<String, Int>? {
    var state = 0
    this.forEachIndexed { index, line ->
        val trimmed = line.trim()
        val noSpaces = trimmed.replace(" ", "")

        if (noSpaces.isEmpty()) {
            return@forEachIndexed
        }

        var processingString = noSpaces

        while (true) {
            val oldState = state

            val (newState, newString) = processingString.scanForGrammarAnnotation(state)
            state = newState
            processingString = newString

            if (oldState == state) {
                break
            }

            if (processingString.isEmpty()) {
                break
            }
        }

        if (state == 5) {
            val firstQuote = trimmed.indexOf('"')
            val secondQuote = trimmed.indexOf('"', startIndex = firstQuote + 1)
            return trimmed.substring((firstQuote + 1) until secondQuote) to index
        }

        if (state == 6) {
            return null
        }
    }

    return null
}

private fun String.scanForGrammarAnnotation(state: ScanState): Pair<ScanState, String> {
    return when (state) {
        0 -> when {
            this.startsWith("/*") -> 7 to this.removePrefix("/*")
            this.startsWith('@') -> 1 to this.substring(1)
            else -> null
        }
        1 -> when {
            this.startsWith("file") -> 2 to this.removePrefix("file")
            else -> null
        }
        2 -> when {
            this.startsWith(':') -> 3 to this.substring(1)
            else -> null
        }
        3 -> when {
            this.startsWith(grammarNameClassSimpleName) -> 4 to this.removePrefix(grammarNameClassSimpleName)
            this.startsWith(grammarNameClassQualifiedName) -> 4 to this.removePrefix(grammarNameClassQualifiedName)
            else -> 0 to "" // Accounting for @file:JvmName or other file-wide annotation
        }
        4 -> when {
            this.startsWith('(') -> 5 to this.substring(1)
            else -> null
        }
        5 -> when {
            this.startsWith('"') && this.indexOf('"', 1) != -1 -> 5 to this
            else -> null
        }
        6 -> null
        7 -> when {
            this.indexOf("*/") != -1 -> 0 to this.substring(0, this.indexOf("*/")).removePrefix("*/")
            else -> state to this
        }
        else -> error("Unknown scan state $state")
    } ?: (6 to this)
}

private fun String.computeFileNameOverride(): String {
    return "${this.replace(Regex(" /"), "_")}.kts"
}

private fun Sequence<String>.mergeBackIntoString(): String {
    return this.joinToString(separator = "\r\n")
}
