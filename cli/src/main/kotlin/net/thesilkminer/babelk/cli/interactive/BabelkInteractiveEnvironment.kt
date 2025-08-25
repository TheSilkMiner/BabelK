package net.thesilkminer.babelk.cli.interactive

import net.thesilkminer.babelk.api.grammar.GrammarPack
import net.thesilkminer.babelk.cli.Console
import net.thesilkminer.babelk.cli.answer
import net.thesilkminer.babelk.cli.cliVersion
import net.thesilkminer.babelk.cli.raw
import net.thesilkminer.babelk.cli.read
import picocli.CommandLine
import kotlin.reflect.full.findAnnotation

internal class BabelkInteractiveEnvironment {
    private companion object {
        private fun createCommandLineBuilder(environment: BabelkInteractiveEnvironment): () -> CommandLine {
            val spec = CommandLine.Model.CommandSpec.create()
                .name("")
                .mixinStandardHelpOptions(false)
                .addSubcommand(LoadGrammarCommand(environment))
                .addSubcommand(ExitGrammarCommand(environment))

            val currentSubcommands = spec.subcommands().toMap()

            // TODO("Add help subcommand")

            return {
                CommandLine(spec)
                    .setExpandAtFiles(false)
            }
        }

        private fun CommandLine.Model.CommandSpec.addSubcommand(command: BabelkInteractiveCommand): CommandLine.Model.CommandSpec {
            return this.addSubcommand(command.name, command.spec.aliases(*command.aliases.toTypedArray()))
        }

        private val BabelkInteractiveCommand.name: String
            get() = this::class.findAnnotation<CommandLine.Command>()?.name ?: error("${this::class.qualifiedName} is not a valid command")

        private val BabelkInteractiveCommand.aliases: List<String>
            get() = this::class.findAnnotation<CommandLine.Command>()?.aliases?.toList() ?: listOf()

        private val BabelkInteractiveCommand.spec: CommandLine.Model.CommandSpec
            get() = CommandLine.Model.CommandSpec.forAnnotatedObject(this).mixinStandardHelpOptions(false)
    }

    private val loadedPacks = mutableMapOf<String, GrammarPack>()
    private val commandLineCreator = createCommandLineBuilder(this)
    private var terminate = false

    fun runInteractiveShell() {
        this.preamble()
        this.promptLoop()
        this.terminate()
    }

    internal fun <R> withGrammarPacks(block: (packs: MutableMap<String, GrammarPack>) -> R): R {
        return block(this.loadedPacks)
    }

    internal fun terminateShell() {
        this.terminate = true
    }

    private fun preamble() {
        Console.raw("Babelk v$cliVersion")
        Console.raw("Copyright (C) 2025  TheSilkMiner")
        Console.raw("This program comes with NO WARRANTY and is licensed to you under the GNU LGPL 3.0")
        Console.raw("For more information on your rights, please refer to https://www.gnu.org/licenses/")
        Console.raw("Your continued usage of this program represents acceptance of the license")
    }

    private fun promptLoop() {
        while (!this.terminate) {
            val command = Console.read()
            val args = command.toArgsList()
            commandLineCreator().execute(*args.toTypedArray())
        }
    }

    private fun String.toArgsList(): List<String> {
        val splitOnSpaces = this.split(' ')
        // If something begins with a quote, we want to merge the entries until they get to an end quote
        return buildList {
            var mergeBuffer = null as StringBuilder?
            for (str in splitOnSpaces) {
                if (str.isEmpty()) continue

                val beginsWithQuote = str.first() == '"'
                val endsWithQuote = str.last() == '"'
                val hasMergeBuffer = mergeBuffer != null

                when {
                    beginsWithQuote && endsWithQuote && !hasMergeBuffer -> add(str.substring(1 until lastIndex))
                    beginsWithQuote && !hasMergeBuffer -> {
                        mergeBuffer = StringBuilder(str)
                        mergeBuffer.deleteAt(0)
                    }
                    endsWithQuote && hasMergeBuffer -> {
                        mergeBuffer.append(' ')
                        mergeBuffer.append(str)
                        mergeBuffer.deleteAt(mergeBuffer.lastIndex)

                        add("$mergeBuffer")
                        mergeBuffer = null
                    }
                    hasMergeBuffer -> mergeBuffer.append(str)
                    else -> add(str)
                }
            }
        }
    }

    private fun terminate() {
        Console.answer("Goodbye")
    }
}
