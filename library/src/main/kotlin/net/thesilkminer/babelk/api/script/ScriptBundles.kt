@file:JvmName("ScriptBundleFactory")

package net.thesilkminer.babelk.api.script

import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.PathWalkOption
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

private class SingleScriptBundle(private val file: ScriptFile) : ScriptBundle {
    override val files: Collection<ScriptFile> get() = listOf(this.file)
    override val size: Int get() = 1
    override fun asMap(): Map<String, ScriptFile> = mapOf(this.file.name to this.file)

    override fun toString(): String = "ScriptBundle[{${this.file}}]"
}

private class MapBasedScriptBundle(files: Collection<ScriptFile>) : ScriptBundle {
    private val bundleFiles = files.associateBy(ScriptFile::name).apply { this.ensureNoDuplicates(files) }

    override val files: Collection<ScriptFile> get() = this.bundleFiles.values
    override fun asMap(): Map<String, ScriptFile> = this.bundleFiles.toMap()

    private fun Map<String, ScriptFile>.ensureNoDuplicates(files: Collection<ScriptFile>) {
        require(this.count() == files.count()) {
            files.groupBy(ScriptFile::name)
                .filterValues { it.count() > 1 }
                .entries
                .joinToString(separator = "\n", prefix = "Found duplicate files within the bundle:\n") { (name, files) -> "- '$name' is used by $files" }
        }
    }

    override fun toString(): String = "ScriptBundle[${this.files}]"
}

@JvmName("createWithFiles")
fun ScriptBundle(files: Collection<ScriptFile>): ScriptBundle {
    return when (files.count()) {
        0 -> error("Unable to create a script bundle without any scripts")
        1 -> SingleScriptBundle(files.single())
        else -> MapBasedScriptBundle(files)
    }
}

@JvmName("createWithFiles")
fun ScriptBundle(files: Iterable<ScriptFile>): ScriptBundle = ScriptBundle(files.toList())

@JvmName("createWithFiles")
fun ScriptBundle(vararg files: ScriptFile): ScriptBundle = ScriptBundle(Iterable { files.iterator() })

@JvmName("fromRootPath")
@JvmOverloads
fun Path.toScriptBundle(
    recurse: Boolean = true,
    charset: Charset = Charsets.UTF_8,
    filterOnlyScripts: Boolean = true,
    nameAssignmentFunction: ((Path) -> String)? = null
): ScriptBundle {
    if (!this.isDirectory()) {
        return ScriptBundle(this.toScriptFile())
    }

    val actualAssignmentFunction = nameAssignmentFunction ?: { null }
    return this.listOrWalk(recurse)
        .filter { it.isRegularFile() }
        .filter { !filterOnlyScripts || "${it.fileName}".endsWith(EXPECTED_SCRIPT_FILE_EXT) }
        .map { it.relativeTo(this) }
        .map { it.toScriptFileWithRoot(this, charset, actualAssignmentFunction) }
        .let { ScriptBundle(Iterable { it.iterator() }) }
}

private fun Path.listOrWalk(recurse: Boolean): Sequence<Path> =
    if (recurse) this.walk(PathWalkOption.INCLUDE_DIRECTORIES) else this.listDirectoryEntries().asSequence()

private fun Path.toScriptFileWithRoot(root: Path, charset: Charset, nameAssignmentFunction: (Path) -> String?): ScriptFile {
    val unrelativizedPath = root / this
    return unrelativizedPath.toScriptFile(
        name = nameAssignmentFunction(this) ?: this.toScriptFile().name, // Leverage existing name-determining algorithm
        charset = charset
    )
}
