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
        val messages = this.buildMessages(throwable, messageProvider)
        val printer = level.printer
        messages.forEach { printer("${level.key} ${this.name}: $it") }
    }

    private fun buildMessages(throwable: Throwable?, messageProvider: () -> String): List<String> {
        return buildList {
            add(messageProvider())
            if (throwable != null) {
                throwable.appendNameMessage(this, null, "")
                throwable.appendStacktrace(this)
            }
        }
    }

    private fun Throwable.appendStacktrace(target: MutableList<String>) {
        if (!this@CliLogger.showStackTraces) return

        val seen = mutableSetOf(this)
        this.stack.forEach { it.appendTo(target, "") }
        this.appendAllEnclosed(target, seen, "")
    }

    private fun Throwable.appendNameMessage(target: MutableList<String>, header: String?, indent: String) {
        target += "$indent${header ?: ""}${this::class.qualifiedName}: ${this.message ?: "<no error message>"}"
    }

    private fun Throwable.appendAllEnclosed(target: MutableList<String>, seen: MutableSet<Throwable>, indent: String) {
        fun <T> List<T>.indexOfFirstIndexed(predicate: (Int, T) -> Boolean): Int? {
            for (i in this.indices) {
                if (predicate(i, this[i])) {
                    return i
                }
            }
            return null
        }

        fun lastDivergingBetween(parent: List<StackTraceElement>, child: List<StackTraceElement>): Int? {
            val reversedParent = parent.asReversed()
            val reversedChild = child.asReversed()

            val firstDiverging = reversedChild.indexOfFirstIndexed { i, element -> element != reversedParent.getOrNull(i) } ?: return null

            return child.lastIndex - firstDiverging
        }

        fun Throwable.appendEnclosed(
            target: MutableList<String>,
            parentStack: List<StackTraceElement>,
            seen: MutableSet<Throwable>,
            header: String,
            indent: String
        ) {
            if (!seen.add(this)) {
                target += "    [circular reference to ${this::class.qualifiedName}]"
                return
            }

            val thisStack = this.stack
            val lastDivergingBetweenIndex = lastDivergingBetween(parentStack, thisStack)

            this.appendNameMessage(target, header, indent)

            if (lastDivergingBetweenIndex != null) {
                (0..lastDivergingBetweenIndex).forEach { thisStack[it].appendTo(target, indent) }
            }

            val commonFrames = thisStack.lastIndex - (lastDivergingBetweenIndex ?: -1)
            if (commonFrames > 0) {
                target += "$indent... $commonFrames more"
            }

            this.appendAllEnclosed(target, seen, indent)
        }

        this.suppressed.forEach { it.appendEnclosed(target, this.stack, seen, "Suppressed: ", "$indent    ") }
        this.cause?.appendEnclosed(target, this.stack, seen, "Caused by: ", indent)
    }

    private fun StackTraceElement.appendTo(target: MutableList<String>, indent: String) {
        target += "$indent    at $this"
    }

    private val Throwable.stack: List<StackTraceElement> get() = this.stackTrace.toList()

    private val LogLevel.key: String
        get() = when (this) {
            LogLevel.ERROR -> "E"
            LogLevel.WARN -> "W"
            LogLevel.INFO -> "I"
            LogLevel.DEBUG -> "D"
        }

    private val LogLevel.printer: (String) -> Unit
        get() = when (this) {
            LogLevel.ERROR, LogLevel.WARN -> Console::rawError
            LogLevel.INFO, LogLevel.DEBUG -> Console::raw
        }
}
