@file:JvmName("ScriptFileFactory")

package net.thesilkminer.babelk.api.script

import java.io.Reader
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.bufferedReader
import kotlin.io.path.readText

private class ReaderProviderBasedScriptFile(override val name: String, private val readerProvider: () -> Reader) : ScriptFile {
    override val location: Path? = null
    override val contentsReader: Reader get() = this.readerProvider()
    override fun toString(): String = "ScriptFile[${this.name} via reader]"
}

private class PathBasedScriptFile(private val path: Path, private val charset: Charset, override val name: String) : ScriptFile {
    override val location: Path get() = this.path
    override val contentsReader: Reader get() = this.path.bufferedReader(this.charset, options = arrayOf(StandardOpenOption.READ))
    override val fullContents: String get() = this.path.readText(this.charset)
    override fun toString(): String = "ScriptFile[${this.name} via ${this.path}]"
}

internal const val EXPECTED_SCRIPT_FILE_EXT = ".grammar.kts"

@JvmName("createWithReader")
fun ScriptFile(name: String, readerProvider: () -> Reader): ScriptFile {
    name.ensureValidName()
    return ReaderProviderBasedScriptFile(name, readerProvider)
}

@JvmName("createFromPath")
@JvmOverloads
fun ScriptFile(path: Path, name: String? = null, charset: Charset = Charsets.UTF_8): ScriptFile {
    val fileName = name ?: path.extractFileName()
    fileName.ensureValidName()
    return PathBasedScriptFile(path, charset, fileName)
}

@JvmSynthetic
fun Path.toScriptFile(name: String? = null, charset: Charset = Charsets.UTF_8): ScriptFile = ScriptFile(this, name, charset)

private fun String.ensureValidName() {
    require(this.isNotBlank()) { this.invalidName("cannot be blank") }
    require(this.endsWith(EXPECTED_SCRIPT_FILE_EXT)) { this.invalidName("must reference a $EXPECTED_SCRIPT_FILE_EXT file") }
    require(!this.contains('\\')) { this.invalidName("cannot contain backslashes (\\)") }
}

private fun String.invalidName(reason: String): String {
    return "Script file name $reason, but got '${this}'"
}

// Logic for automatic determination of file name:
// 1. If the path is absolute, then extract the file name only and use that
// 2. If the path is relative, then we want to use the full path with some changes
//    - the path must be normalizable (if the path is not normalizable, it's an error)
//    - the path must use forward slashes instead of whatever the separator is
private fun Path.extractFileName(): String {
    if (this.isAbsolute) {
        return "${this.fileName}"
    }

    val normalized = this.normalize()

    val self = normalized.fileSystem.getPath(".")
    val parent = normalized.fileSystem.getPath("..")

    return normalized.asSequence()
        .filterNot { it == self } // We can ignore those
        .onEach { if (it == parent) error("Found non-normalized path element $parent: this is not allowed") }
        .joinToString(separator = "/")
}
