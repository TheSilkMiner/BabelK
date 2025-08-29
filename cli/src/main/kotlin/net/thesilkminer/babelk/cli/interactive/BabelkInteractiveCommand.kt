package net.thesilkminer.babelk.cli.interactive

import net.thesilkminer.babelk.cli.Console
import net.thesilkminer.babelk.cli.raiseException

internal sealed class BabelkInteractiveCommand(protected val environment: BabelkInteractiveEnvironment) : Runnable {
    final override fun run() {
        runCatching { this.execute() }.onFailure { this.reportFailure(it) }
    }

    abstract fun execute()

    private fun reportFailure(throwable: Throwable) {
        Console.raiseException(throwable, "An error occurred when executing the target command")
    }
}
