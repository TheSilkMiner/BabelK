@file:JvmName("BuildingContextCapturingDsl")

package net.thesilkminer.babelk.script.api.invoke

// Note: we are providing this DSL here instead of in the script-dsl package as this DSL is actually quite useful
// for raw implementation of rules, rather than only for scripts

interface BuildingContextCaptureDsl {
    var invocationMode: CaptureInvocationMode
    var stackOverflowMode: CaptureStackOverflowMode

    fun capture(block: (context: BuildingContext) -> Unit)
    fun andThen(block: BuildingContext.(events: BuildingEventList) -> Unit)
}

private class ContextCapture(private val context: BuildingContext, mode: CaptureMode) : BuildingContextCaptureDsl {
    override var invocationMode: CaptureInvocationMode = mode.invocationMode
    override var stackOverflowMode: CaptureStackOverflowMode = mode.stackOverflowMode

    private var capture: ((BuildingContext) -> Unit)? = null
    private var andThen: (BuildingContext.(BuildingEventList) -> Unit)? = null

    private val mode: CaptureMode get() = CaptureMode(this.invocationMode, this.stackOverflowMode)

    override fun capture(block: (context: BuildingContext) -> Unit) {
        require(this.capture == null) { "Capture function has already been provided" }
        this.capture = block
    }

    override fun andThen(block: BuildingContext.(events: BuildingEventList) -> Unit) {
        require(this.andThen == null) { "Follow-up function has already been provided" }
        this.andThen = block
    }

    operator fun invoke() {
        val capture = requireNotNull(this.capture) { "Capture function has not been provided" }
        val andThen = requireNotNull(this.andThen) { "Follow-up function has not been provided" }

        with(this.context) {
            val events = this.capture(this@ContextCapture.mode, capture)
            this.andThen(events)
        }
    }
}

fun BuildingContext.capturing(mode: CaptureMode = CaptureMode(), block: BuildingContextCaptureDsl.() -> Unit) {
    ContextCapture(this, mode).apply(block)()
}

fun BuildingContextCaptureDsl.andThenWithAppends(block: BuildingContext.(literals: List<AppendLiteral>) -> Unit) {
    this.andThen { block(it.filterAllAppends()) }
}

fun BuildingContextCaptureDsl.andThenWithConcatenation(separator: String = "", block: BuildingContext.(result: String) -> Unit) {
    this.andThen { block(it.concatenateAppends(separator)) }
}
