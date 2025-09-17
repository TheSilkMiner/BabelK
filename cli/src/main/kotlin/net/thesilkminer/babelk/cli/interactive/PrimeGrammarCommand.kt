package net.thesilkminer.babelk.cli.interactive

import net.thesilkminer.babelk.api.grammar.GrammarPack
import net.thesilkminer.babelk.api.grammar.GrammarRule
import net.thesilkminer.babelk.api.invoke.BuiltinStackOverflowCallbacks
import net.thesilkminer.babelk.api.invoke.InvocationConfigurationDsl
import net.thesilkminer.babelk.api.invoke.RandomSource
import net.thesilkminer.babelk.api.invoke.SequenceConfigurationDsl
import net.thesilkminer.babelk.api.invoke.StackOverflowCallback
import net.thesilkminer.babelk.cli.Console
import net.thesilkminer.babelk.cli.answer
import net.thesilkminer.babelk.cli.raiseError
import picocli.CommandLine

@CommandLine.Command(name = "prime")
internal class PrimeGrammarCommand(environment: BabelkInteractiveEnvironment) : BabelkInteractiveCommand(environment) {
    private sealed interface StackOverflowCallbackType {
        val callback: StackOverflowCallback

        fun apply(configuration: SequenceConfigurationDsl) {
            configuration.onStackOverflow(this@StackOverflowCallbackType.callback)
        }
    }

    private object ThrowExceptionStackOverflowCallbackType : StackOverflowCallbackType {
        override val callback: StackOverflowCallback get() = BuiltinStackOverflowCallbacks.throwException
    }

    private object IgnoreStackOverflowCallbackType : StackOverflowCallbackType {
        override val callback: StackOverflowCallback get() = BuiltinStackOverflowCallbacks.ignore
    }

    private object ReplaceWithNameStackOverflowCallbackType : StackOverflowCallbackType {
        override val callback: StackOverflowCallback get() = BuiltinStackOverflowCallbacks.replaceWithName
    }

    private class ReplaceWithStringStackOverflowCallbackType(private val string: String) : StackOverflowCallbackType {
        override val callback: StackOverflowCallback get() = { _, _ -> this.string }
    }

    private class StackOverflowCallbackTypeArgumentConverter : CommandLine.ITypeConverter<StackOverflowCallbackType> {
        override fun convert(value: String?): StackOverflowCallbackType? {
            if (value == null) return null

            val typeString = value.substringBefore(':')
            val argument = if (typeString == value) null else value.substringAfter(':')
            return this.interpret(typeString, argument)
        }

        private fun interpret(type: String, argument: String?): StackOverflowCallbackType {
            return when (type) {
                "ignore", "ignore_rule", "discard" -> IgnoreStackOverflowCallbackType
                "throw", "throw_exception" -> ThrowExceptionStackOverflowCallbackType
                "replace" -> when (argument) {
                    null -> throw CommandLine.TypeConversionException("Overflow callback type 'replace' requires an argument but none specified")
                    $$"$rule_name", $$"$name" -> ReplaceWithNameStackOverflowCallbackType
                    else -> ReplaceWithStringStackOverflowCallbackType(argument)
                }
                else -> throw CommandLine.TypeConversionException("Unable to parse '$type${argument?.let { ":$it" } ?: ""}' because such a callback is unknown")
            }
        }
    }

    private class InvocationStackDepthArgumentConverter : CommandLine.ITypeConverter<Int> {
        override fun convert(value: String?): Int? {
            if (value == null) return null
            val valueAsInt = value.toIntOrNull() ?: throw CommandLine.TypeConversionException("'$value' is not an integer")
            if (valueAsInt < 1) {
                throw CommandLine.TypeConversionException("Maximum stack depth must be at least one")
            }
            return valueAsInt
        }
    }

    private class RandomSourceArgumentConverter : CommandLine.ITypeConverter<RandomSource> {
        override fun convert(value: String?): RandomSource? {
            if (value == null) return null
            val valueAsLong = value.toLongOrNull() ?: value.toLongOrNull(radix = 16)
            return valueAsLong?.let(::RandomSource) ?: RandomSource(value.hashCode())
        }
    }

    @CommandLine.Parameters(
        index = "0",
        paramLabel = "pack",
        description = ["The name of the grammar pack whose rule should be queried"]
    )
    private lateinit var grammarPackName: String

    @CommandLine.Parameters(
        index = "1",
        paramLabel = "rule",
        description = [
            "The rule of the given grammar whose invocation should be primed",
            "The rule is to be expressed in 'rule path' format, which is a string with of the format \"grammar:rule\",",
            "where \"grammar\" specifies the name of the grammar in the pack that should be queried and \"rule\" the",
            "name of the rule within the given grammar that should be queried.",
            "As a special case, if the specified grammar pack contains only a single grammar, then only the rule",
            "may be specified: the grammar name will be automatically inferred"
        ]
    )
    private lateinit var rulePath: String

    @CommandLine.Parameters(
        index = "2",
        arity = "0..1",
        paramLabel = "primedName",
        description = [
            "Specifies the name that should be used to store the primed sequence for future queries",
            "This name must then be used for invoking the rule in order to get the corresponding outputs.",
            "If no name is specified, then it is defaulted to the rule path specified to invoke the rule, with the",
            "grammar name prefixed if none had been specified.",
            "If an existing primed sequence for the rule already exists, then the pre-existing sequence will be",
            "discarded and this sequence will be stored instead. If such a case should raise an error, then specify",
            "the --no-overwrite command line flag when invoking this command"
        ]
    )
    private var primedRuleName: String? = null

    @CommandLine.Option(
        names = ["--no-overwrite"],
        description = ["If specified and a primed sequence with the given name already exists, the priming will fail"],
        arity = "0"
    )
    private var noOverwrite: Boolean = false

    @CommandLine.Option(
        names = ["-d", "--max-stack", "--stack", "--stack-depth", "--depth"],
        description = [
            "Indicates the maximum stack space allowed for the expansion of the target rule",
            "The stack space represents how deep the rule can travel before its expansion is truncated. The truncation",
            "behavior may be configured via the -c option.",
            "The provided value must be at least 1. Note that values that are too small might prevent the rule from",
            "expanding in a satisfactory manner."
        ],
        defaultValue = "${Int.MAX_VALUE}",
        converter = [InvocationStackDepthArgumentConverter::class]
    )
    private var invocationStackDepth: Int = Int.MAX_VALUE

    @CommandLine.Option(
        names = ["-s", "--seed"],
        description = [
            "Specifies the seed used for random calls within the rules",
            "If unspecified, the default seed as determined by the implementation will be used."
        ],
        converter = [RandomSourceArgumentConverter::class]
    )
    private var randomSource: RandomSource? = null

    @CommandLine.Option(
        names = ["-a", "--arg"],
        arity = "0..*",
        description = [
            "Specifies the arguments that must be provided to the rule for its invocation",
            "The format is to be specified as \"name=value\", and the values will be interpreted as raw strings.",
            "Multiple values may be specified by specifying the -a flag multiple times."
        ]
    )
    private var arguments: Map<String, String>? = null

    @CommandLine.Option(
        names = ["-c", "--callback", "--stack-overflow-callback"],
        description = [
            "Indicates what to do in case of a stack overflow as determined by the -d flag",
            "The value is of the form \"type[:argument]\", where \"type\" indicates the desired behavior and",
            "\"argument\" is additional optional configuration that depends on the given type.",
            "Allowed types are the following:",
            "- ignore: do not expand the rule and simply carry on with the next step, this is similar to replacing the",
            "          affected rule with an empty rule;",
            "- throw: throw an exception detailing the rule failure, which will be reported on the console;",
            "- replace: replace the entire rule invocation with the string provided as an argument, in particular the",
            "           string \$rule_name can be used to reference the name of the rule that was invoked (if any),",
            "           which might aid debugging."
        ],
        converter = [StackOverflowCallbackTypeArgumentConverter::class]
    )
    @Suppress("CanConvertToMultiDollarString") // Messes up the alignment
    private var stackOverflowCallback: StackOverflowCallbackType? = null

    override fun execute() {
        Console.answer("Priming rule '${this.rulePath}' of pack '${this.grammarPackName}' for invocation")

        val pack = this.environment.withGrammarPacks { it[this.grammarPackName] }

        if (pack == null) {
            Console.raiseError("No pack with name '${this.grammarPackName}' was found")
            return
        }

        val path = this.rulePath.fixFor(pack) ?: return
        val rule = pack.getRuleSafe(path) ?: return

        val sequenceName = this.primedRuleName ?: path
        if (this.noOverwrite && this.environment.withPrimedSequences { sequenceName in it.keys }) {
            Console.raiseError("A primed rule with the name '$sequenceName' already exists and overwriting has been disabled")
            return
        }

        val sequence = rule { this.configure() }
        this.environment.withPrimedSequences { it[sequenceName] = sequence }
        Console.answer("Successfully primed rule: sequence stored as $sequenceName")
    }

    private fun String.fixFor(pack: GrammarPack): String? {
        if (':' in this) return this

        val grammarName = pack.grammars.singleOrNull()?.name ?: run {
            Console.raiseError("The given rule path '$this' has no grammar name, but pack " +
                "'${this@PrimeGrammarCommand.grammarPackName}' has more than one grammar: automatic inference is unavailable")
            return null
        }

        return "$grammarName:$this"
    }

    private fun GrammarPack.getRuleSafe(path: String): GrammarRule? {
        return runCatching { this.getRule(path) }
            .onFailure { Console.raiseError(it.message ?: "An unknown error occurred while trying to obtain the target rule") }
            .getOrNull()
    }

    private fun InvocationConfigurationDsl.configure() {
        sequence {
            this.stackDepth = this@PrimeGrammarCommand.invocationStackDepth
            this@PrimeGrammarCommand.stackOverflowCallback?.apply(this)
        }
        this@PrimeGrammarCommand.randomSource?.let { this.randomSource = it }
        arguments(this@PrimeGrammarCommand.arguments ?: mapOf())
    }
}
