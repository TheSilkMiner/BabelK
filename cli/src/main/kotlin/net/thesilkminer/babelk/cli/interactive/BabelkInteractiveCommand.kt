package net.thesilkminer.babelk.cli.interactive

import net.thesilkminer.babelk.cli.CliLoggerFactory

internal sealed class BabelkInteractiveCommand(protected val environment: BabelkInteractiveEnvironment) : Runnable {
    final override fun run() {
        runCatching { this.execute() }.onFailure { this.reportFailure(it) }
    }

    abstract fun execute()

    private fun reportFailure(throwable: Throwable) {
        // Leverage logger factory to use the same formatting
        val logger = CliLoggerFactory().newLogger("Shell")
        logger.error(throwable) { "An error occurred when executing the target command" }
    }
}
