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
