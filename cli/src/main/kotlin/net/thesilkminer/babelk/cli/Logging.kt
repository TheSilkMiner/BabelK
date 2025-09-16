@file:JvmName("Logging")

package net.thesilkminer.babelk.cli

import net.thesilkminer.babelk.api.LogLevel
import net.thesilkminer.babelk.api.Logger
import net.thesilkminer.babelk.api.LoggerFactory

internal data class LoggingConfiguration(val logLevel: RequestedLogLevel, val showStackTraces: Boolean) {
    companion object {
        lateinit var instance: LoggingConfiguration
            internal set
    }
}

internal class CliLoggerFactory : LoggerFactory {
    override fun newLogger(name: String): Logger =
        CliLogger(
            name,
            LoggingConfiguration.instance.logLevel.level,
            LoggingConfiguration.instance.showStackTraces
        )

    private val RequestedLogLevel.level: LogLevel?
        get() = when (this) {
            RequestedLogLevel.OFF -> null
            RequestedLogLevel.ERROR -> LogLevel.ERROR
            RequestedLogLevel.WARNING -> LogLevel.WARN
            RequestedLogLevel.INFO -> LogLevel.INFO
            RequestedLogLevel.DEBUG -> LogLevel.DEBUG
        }
}

private class CliLogger(private val name: String, private val minLevel: LogLevel?, private val showStackTraces: Boolean) : Logger {
    override fun log(level: LogLevel, throwable: Throwable?, messageProvider: () -> String) {
        if (this.minLevel == null || level >= this.minLevel) return
        Console.log(throwable, level.isError, level.marker, this.showStackTraces) { "$name: ${messageProvider()}" }
    }

    private val LogLevel.isError: Boolean get() = this.ordinal <= LogLevel.WARN.ordinal

    private val LogLevel.marker: Char?
        get() = when (this) {
            LogLevel.ERROR -> null
            LogLevel.WARN -> 'W'
            LogLevel.INFO -> null
            LogLevel.DEBUG -> 'D'
        }
}
