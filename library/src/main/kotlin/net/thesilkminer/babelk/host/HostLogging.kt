@file:JvmName("HostLogging")

package net.thesilkminer.babelk.host

import net.thesilkminer.babelk.api.Logger
import net.thesilkminer.babelk.script.host.Log
import net.thesilkminer.babelk.script.host.LogCreator

private class HostLogger(name: String) : Log {
    private val logger = Logger(name)

    override fun error(throwable: Throwable?, messageProvider: () -> String) = this.logger.error(throwable, messageProvider)
    override fun warn(throwable: Throwable?, messageProvider: () -> String) = this.logger.warn(throwable, messageProvider)
    override fun info(throwable: Throwable?, messageProvider: () -> String) = this.logger.info(throwable, messageProvider)
    override fun debug(throwable: Throwable?, messageProvider: () -> String) = this.logger.info(throwable, messageProvider)
}

internal class HostLogCreator : LogCreator {
    override fun new(name: String): Log = HostLogger(name)
}
