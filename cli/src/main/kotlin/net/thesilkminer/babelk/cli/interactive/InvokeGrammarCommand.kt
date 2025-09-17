package net.thesilkminer.babelk.cli.interactive

import net.thesilkminer.babelk.api.invoke.RuleInvocation
import net.thesilkminer.babelk.cli.Console
import net.thesilkminer.babelk.cli.answer
import net.thesilkminer.babelk.cli.raiseError
import net.thesilkminer.babelk.cli.raw
import picocli.CommandLine
import java.util.Locale

@CommandLine.Command(name = "invoke")
internal class InvokeGrammarCommand(environment: BabelkInteractiveEnvironment) : BabelkInteractiveCommand(environment) {
    private enum class OutputType(private val key: String, private val printer: (RuleInvocation) -> Unit) {
        ALL("all", Console::answer, { "${it.sequenceNumber}: ${it.result}" }), // > 1: Foo
        STRING("string", Console::answer, RuleInvocation::result), // > Foo
        RAW("raw", Console::raw, RuleInvocation::result); // Foo

        constructor(key: String, target: (String) -> Unit, mapper: (RuleInvocation) -> String) : this(key, { target(mapper(it)) })

        fun print(invocation: RuleInvocation) = this.printer(invocation)

        operator fun contains(value: String) = value == this.key
    }

    private class OutputTypeArgumentConverter : CommandLine.ITypeConverter<OutputType> {
        override fun convert(value: String?): OutputType? {
            if (value == null) return null
            val lowercased = value.lowercase(Locale.ENGLISH)
            return OutputType.entries.firstOrNull { lowercased in it }
                ?: throw CommandLine.TypeConversionException(OutputType.entries.joinToString(prefix = "Unrecognized option '$value': must be one of "))
        }
    }

    private class QuantityArgumentConverter : CommandLine.ITypeConverter<Int> {
        override fun convert(value: String?): Int? {
            if (value == null) return null
            val valueAsInt = value.toIntOrNull() ?: throw CommandLine.TypeConversionException("'$value' is not an integer")
            if (valueAsInt < 1) {
                throw CommandLine.TypeConversionException("Quantity must be at least one")
            }
            return valueAsInt
        }
    }

    @CommandLine.Parameters(
        index = "0",
        paramLabel = "sequence",
        description = ["The name of the sequence primed with the prime command that should be invoked"]
    )
    private lateinit var sequenceName: String

    @CommandLine.Parameters(
        index = "1",
        arity = "0..1",
        paramLabel = "quantity",
        defaultValue = "1",
        description = ["The amount of strings that should be obtained from the given sequence"],
        converter = [QuantityArgumentConverter::class]
    )
    private var quantity: Int = 1

    @CommandLine.Option(
        names = ["--output-type", "-o"],
        description = [
            "Indicates how the result will be printed, specifying the information and the formatting",
            "The value can be any of the following:",
            "- all: the string will be formatted and additional information (such as the sequence number of the",
            "       string) will also be provided;",
            "- string: the string will be formatted, but no additional information will be provided;",
            "- raw: the string will be printed as-is, without formatting."
        ],
        defaultValue = "string",
        converter = [OutputTypeArgumentConverter::class]
    )
    private var outputType: OutputType = OutputType.STRING

    override fun execute() {
        Console.answer("Invoking sequence '${this.sequenceName}' for ${this.quantity} strings")

        val sequence = this.environment.withPrimedSequences { it[this.sequenceName] }
        if (sequence == null) {
            Console.raiseError("No sequence with name '${this.sequenceName}' found: did you prime it?")
            return
        }

        val strings = sequence.next(this.quantity)
        strings.forEach(this.outputType::print)
    }
}
