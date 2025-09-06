package net.thesilkminer.babelk.script.api.invoke

interface BuildingContext {
    fun append(literal: String)
    fun invoke(rule: InvokableRule)

    fun capture(mode: CaptureMode = CaptureMode(), block: (context: BuildingContext) -> Unit): BuildingEventList
}
