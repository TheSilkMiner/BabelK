package net.thesilkminer.babelk.cli.interactive

import net.thesilkminer.babelk.cli.Console
import net.thesilkminer.babelk.cli.answer
import picocli.CommandLine
import java.util.Locale

@CommandLine.Command(name = "show", aliases = ["list"])
internal class ShowGrammarCommand(environment: BabelkInteractiveEnvironment) : BabelkInteractiveCommand(environment) {
    private enum class ObjectType(private val aliases: Set<String>) {
        PACKS("packs"),
        GRAMMARS("grammars"),
        RULES("rules"),
        SEQUENCES("sequences", "invocations");

        constructor(vararg grammars: String) : this(grammars.toSet())
        @Suppress("UNUSED") constructor() : this(error("At least one alias must be specified") as Set<String>)

        operator fun contains(value: String) = value in this.aliases
        override fun toString(): String = this.aliases.first()
    }

    private class ObjectTypeArgumentConverter : CommandLine.ITypeConverter<ObjectType> {
        override fun convert(value: String?): ObjectType? {
            if (value == null) return null
            val lowercased = value.lowercase(Locale.ENGLISH)
            return ObjectType.entries.firstOrNull { lowercased in it }
                ?: throw CommandLine.TypeConversionException(ObjectType.entries.joinToString(prefix = "Unrecognized option '$value': must be one of "))
        }
    }

    @CommandLine.Parameters(
        index = "0",
        paramLabel = "objectType",
        description = ["The type of the objects that should be shown on screen"],
        converter = [ObjectTypeArgumentConverter::class]
    )
    private lateinit var objectType: ObjectType

    // TODO("in option (e.g. show grammars --in test)")

    override fun execute() {
        Console.answer("Listing all objects of type ${this.objectType}")
        when (this.objectType) {
            ObjectType.PACKS -> this.listMapKeys { this::withGrammarPacks }
            ObjectType.GRAMMARS -> this.listMapKeys { { this.withGrammarNames(it) } }
            ObjectType.RULES -> this.listMapKeys { { this.withRuleNames(it) } }
            ObjectType.SEQUENCES -> this.listMapKeys { this::withPrimedSequences }
        }
    }

    private fun <T> listMapKeys(mapProvider: BabelkInteractiveEnvironment.() -> ((MutableMap<String, T>) -> Unit) -> Unit) {
        this.environment.mapProvider().invoke { it.keys.forEach { name -> Console.answer("- $name") } }
    }

    private fun BabelkInteractiveEnvironment.withGrammarNames(block: (MutableMap<String, Unit>) -> Unit) {
        return this.withGrammarPacks { packs ->
            packs.entries
                .asSequence()
                .flatMap { (name, pack) -> pack.grammars.asSequence().map { "${it.name} (in pack $name)" } }
                .associateWithTo(mutableMapOf()) {}
                .let(block)
        }
    }

    private fun BabelkInteractiveEnvironment.withRuleNames(block: (MutableMap<String, Unit>) -> Unit) {
        return this.withGrammarPacks { packs ->
            packs.entries
                .asSequence()
                .flatMap { (name, pack) ->
                    pack.grammars
                        .asSequence()
                        .flatMap { grammar -> grammar.rules.asSequence().map { "${grammar.name}:${it.name} (in pack $name)" } }
                }
                .associateWithTo(mutableMapOf()) {}
                .let(block)
        }
    }
}
