@file:JvmName("Logging")

package net.thesilkminer.babelk.script.host

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface LogCreator {
    companion object {
        private val lock = ReentrantLock()

        var instance: LogCreator? = null
            get() = this.lock.withLock { field }
            set(value) = this.lock.withLock {
                require(field == null) { "Log Creator instance was already set" }
                field = value
            }
    }

    fun new(name: String): Log
}

interface Log {
    fun error(throwable: Throwable? = null, messageProvider: () -> String)
    fun warn(throwable: Throwable? = null, messageProvider: () -> String)
    fun info(throwable: Throwable? = null, messageProvider: () -> String)
    fun debug(throwable: Throwable? = null, messageProvider: () -> String)
}

private class DelayedLog(name: String) : Log {
    private val logger by lazy { LogCreator.instance?.new(name) ?: error("Log creator instance was never set before any message could be logged") }

    override fun error(throwable: Throwable?, messageProvider: () -> String) = this.logger.error(throwable, messageProvider)
    override fun warn(throwable: Throwable?, messageProvider: () -> String) = this.logger.warn(throwable, messageProvider)
    override fun info(throwable: Throwable?, messageProvider: () -> String) = this.logger.info(throwable, messageProvider)
    override fun debug(throwable: Throwable?, messageProvider: () -> String) = this.logger.debug(throwable, messageProvider)
}

fun Log(name: String): Log = LogCreator.instance?.new(name) ?: DelayedLog(name)
fun Log(block: () -> Unit): Log = Log(block.findName())

private fun (() -> Unit).findName(): String {
    val fullClassName = this.javaClass.name
    val simpleName = fullClassName.substringAfterLast('.')
    val topLevelName = simpleName.substringBefore('$')
    return topLevelName
}
