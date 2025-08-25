package net.thesilkminer.babelk.cli.interactive

import picocli.CommandLine

@CommandLine.Command(name = "exit", aliases = ["quit"])
internal class ExitGrammarCommand(environment: BabelkInteractiveEnvironment) : BabelkInteractiveCommand(environment) {
    override fun execute() {
        this.environment.terminateShell()
    }
}
