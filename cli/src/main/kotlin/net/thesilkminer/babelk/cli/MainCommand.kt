@file:JvmName("MainCommand")

package net.thesilkminer.babelk.cli

import net.thesilkminer.babelk.cli.interactive.BabelkInteractiveEnvironment
import picocli.CommandLine

@CommandLine.Command(
    name = "babelk",
    versionProvider = BabelkVersionProvider::class,
    mixinStandardHelpOptions = true,
    headerHeading = "BabelK - String generation tool powered by a contextual grammar%nLicensed under: GNU LGPL v3",
    header = ["Starts an interactive BabelK session"],
    showDefaultValues = true
)
internal class BabelkCommand : Runnable {
    @CommandLine.Option(
        names = ["--stacktrace"],
        description = [
            "If specified, logs stack traces to the usual corresponding logging output based on the log level.",
            "Stack traces are usually logged in case of error conditions and contain debug output useful to programmers.",
            "If you are a user, this may be useful in order to obtain logs to report bugs."
        ],
        arity = "0"
    )
    private var enableStackTraces: Boolean = false

    @CommandLine.Option(
        names = ["--log", "--log-level"],
        description = [
            "Sets the level of logging that will be output to the console during execution of the various commands.",
            "It can be set to any of 'off', 'error', 'warning', 'info', 'debug', in increased order of verbosity.",
            "Note that this does not affect non-logging related output, such as rule invocation results."
        ],
        defaultValue = "warning",
        showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
        converter = [RequestedLogLevelConversionProvider::class]
    )
    private var logLevel: RequestedLogLevel = RequestedLogLevel.WARNING

    override fun run() {
        this.setUpLogging()
        BabelkInteractiveEnvironment().runInteractiveShell()
    }

    private fun setUpLogging() {
        LoggingConfiguration.instance = LoggingConfiguration(this.logLevel, this.enableStackTraces)
    }
}

internal enum class RequestedLogLevel {
    OFF,
    ERROR,
    WARNING,
    INFO,
    DEBUG
}

private class RequestedLogLevelConversionProvider : CommandLine.ITypeConverter<RequestedLogLevel> {
    override fun convert(value: String?): RequestedLogLevel? {
        if (value == null) return null
        return RequestedLogLevel.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

private class BabelkVersionProvider : CommandLine.IVersionProvider {
    override fun getVersion(): Array<out String> {
        return arrayOf(cliVersion)
    }
}

