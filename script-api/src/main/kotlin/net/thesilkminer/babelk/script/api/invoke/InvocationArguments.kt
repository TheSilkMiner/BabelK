package net.thesilkminer.babelk.script.api.invoke

interface InvocationArguments {
    val allArguments: Set<ArgumentName>

    fun argumentValue(name: ArgumentName): InvokableRule?
}
