package net.thesilkminer.babelk.cli.interactive

import net.thesilkminer.babelk.api.compileToGrammarPack
import net.thesilkminer.babelk.api.script.ScriptBundle
import net.thesilkminer.babelk.api.script.ScriptFile
import net.thesilkminer.babelk.api.script.toScriptBundle
import net.thesilkminer.babelk.cli.Console
import net.thesilkminer.babelk.cli.answer
import net.thesilkminer.babelk.cli.yesNo
import picocli.CommandLine
import java.nio.file.Path
import kotlin.io.path.Path

@CommandLine.Command(name = "load")
internal class LoadGrammarCommand(environment: BabelkInteractiveEnvironment) : BabelkInteractiveCommand(environment) {
    private class FileCollectionEntry(val path: Path, val shouldRecurse: Boolean)

    private class FilesCollectionTypeArgumentConverter : CommandLine.ITypeConverter<FileCollectionEntry> {
        override fun convert(value: String?): FileCollectionEntry? {
            if (value == null) return null

            val parts = value.split(Regex("[/\\\\]"))

            if (parts.isEmpty()) throw CommandLine.TypeConversionException("Unable to convert '$parts' to a collection as it is empty")

            val last = parts.last()
            val isSingle = parts.count() == 1

            val isSingleGlob = last == "*"
            val isDoubleGlob = last == "**"
            val isGlobIndicator = isSingleGlob || isDoubleGlob

            val hasInitialDirectory = !isSingle || !isGlobIndicator

            val root = if (hasInitialDirectory) Path(parts.first()) else Path(".")
            val lastResolvable = parts.count() - (if (isGlobIndicator) 1 else 0)
            val target = parts.subList(1, lastResolvable).fold(root, Path::resolve)

            val isFullRecurse = isDoubleGlob || !isSingleGlob

            return FileCollectionEntry(target, isFullRecurse)
        }
    }

    @CommandLine.Parameters(
        index = "0",
        paramLabel = "name",
        description = ["The name of the grammar pack to load"]
    )
    private lateinit var name: String

    @CommandLine.Parameters(
        index = "1..*",
        arity = "1..*",
        paramLabel = "files",
        description = [
            "The grammar scripts that must be executed to construct the grammar pack",
            "They can be either absolute paths, relative paths (from the current working directory), or file filters",
            "Note that specifying a directory will implicitly try to read all scripts inside the given directory"
        ],
        converter = [FilesCollectionTypeArgumentConverter::class]
    )
    private lateinit var files: Collection<FileCollectionEntry>

    override fun execute() {
        Console.answer("Attempting to load grammar pack named '$name'")

        if (this.environment.withGrammarPacks { name in it }) {
            val overwrite = Console.yesNo("A grammar pack named '$name' already exists, overwrite?")
            if (!overwrite) {
                return
            }
        }

        val bundle = this.files.toBundle()
        val pack = bundle.compileToGrammarPack()

        this.environment.withGrammarPacks { it[name] = pack }

        Console.answer("Successfully loaded grammar pack named '$name'")
    }

    private fun Collection<FileCollectionEntry>.toBundle(): ScriptBundle {
        return when (this.count()) {
            0 -> error("Empty sequence not supported")
            1 -> this.single().toBundle()
            else -> this.asSequence()
                .map { it.toBundle() }
                .map { it.files.asSequence() }
                .reduce(Sequence<ScriptFile>::plus)
                .asIterable()
                .let(::ScriptBundle)
        }
    }

    private fun FileCollectionEntry.toBundle(): ScriptBundle {
        // TODO("Maybe some name assignment logic?")
        return this.path.toScriptBundle(recurse = this.shouldRecurse)
    }
}
