// ============================ Note ============================
// This is meant to be a 1:1 copy of the equivalent file in the main source set to allow Kotlin code in this
// source set access. The Kotlin compiler offers no --patch-module equivalent command that I could find, so
// this is the fastest way to fix the issue without relying on undocumented compiler internals. At least
// until another solution is found, this will remain.
// ==============================================================
@file:JvmName("Logging")

package net.thesilkminer.babelk.api

import java.util.ServiceLoader

private val loggerFactory by lazy { ServiceLoader.load(LoggerFactory::class.java).first() }

interface LoggerFactory {
    fun newLogger(name: String): Logger
}

enum class LogLevel {
    ERROR,
    WARN,
    INFO,
    DEBUG
}

interface Logger {
    fun log(level: LogLevel, throwable: Throwable? = null, messageProvider: () -> String)

    fun error(throwable: Throwable? = null, messageProvider: () -> String) = this.log(LogLevel.ERROR, throwable, messageProvider)
    fun warn(throwable: Throwable? = null, messageProvider: () -> String) = this.log(LogLevel.WARN, throwable, messageProvider)
    fun info(throwable: Throwable? = null, messageProvider: () -> String) = this.log(LogLevel.INFO, throwable, messageProvider)
    fun debug(throwable: Throwable? = null, messageProvider: () -> String) = this.log(LogLevel.DEBUG, throwable, messageProvider)
}

fun Logger(name: String): Logger = loggerFactory.newLogger(name)
fun Logger(block: () -> Unit): Logger = Logger(block.findName())

private fun (() -> Unit).findName(): String {
    val fullClassName = this.javaClass.name
    val simpleName = fullClassName.substringAfterLast('.')
    val topLevelName = simpleName.substringBefore('$')
    return topLevelName
}
