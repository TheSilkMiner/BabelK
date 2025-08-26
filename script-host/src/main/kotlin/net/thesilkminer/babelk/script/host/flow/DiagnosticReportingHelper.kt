@file:JvmName("DiagnosticReportingHelper")

package net.thesilkminer.babelk.script.host.flow

import net.thesilkminer.babelk.script.host.Log
import java.io.File
import java.util.Locale
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.valueOr
import kotlin.text.substringAfterLast

internal fun <R> ResultWithDiagnostics<R>.reportDiagnostics(log: Log): ResultWithDiagnostics<R> {
    this.reports.forEach { (code, message, severity, sourcePath, location, exception) ->
        log.withSeverity(severity, exception) {
            buildString {
                append("  [")
                append(severity.name.let { "${it[0]}${it.substring(1).lowercase(Locale.ENGLISH)}" })
                append(" 0x")
                append(code.toULong().toString(radix = 16).padStart(16, '0'))
                append(']')

                if (sourcePath != null) {
                    append(" in ")
                    append(sourcePath.substringAfterLast(File.pathSeparatorChar))
                }

                if (location != null) {
                    append(" at ")
                    append(location.start.line)
                    append(':')
                    append(location.start.col)
                }

                append(": ")
                append(message)

                if (exception != null) {
                    append(" (this caused a ")
                    append(exception::class.qualifiedName)
                    append(": ")
                    append(exception.message ?: "<no error message>")
                    append(')')
                }
            }
        }
    }
    return this
}

internal fun <R> ResultWithDiagnostics<R>.rethrowOnError(log: Log, messageProvider: () -> String): R {
    return this.valueOr { _ ->
        val message = messageProvider()
        val exception = RuntimeException(message)
        log.error(exception) { message }
        throw exception
    }
}

private fun Log.withSeverity(severity: ScriptDiagnostic.Severity, throwable: Throwable?, messageProvider: () -> String) {
    when (severity) {
        ScriptDiagnostic.Severity.DEBUG -> this.debug(throwable, messageProvider)
        ScriptDiagnostic.Severity.INFO -> this.info(throwable, messageProvider)
        ScriptDiagnostic.Severity.WARNING -> this.warn(throwable, messageProvider)
        ScriptDiagnostic.Severity.ERROR -> this.error(throwable, messageProvider)
        ScriptDiagnostic.Severity.FATAL -> this.error(throwable) { "FATAL: ${messageProvider()}" }
    }
}
