@file:JvmName("BuildingEvents")

package net.thesilkminer.babelk.script.api.invoke

import kotlin.reflect.KClass
import kotlin.reflect.safeCast

typealias BuildingEventList = List<BuildingEvent>

enum class CaptureInvocationMode {
    RECURSE_AND_KEEP, // (Pre|Post)InvokeSubRule events are retained, the invocation is carried forward with its events captured
    RECURSE_ONLY, // (Pre|Post)InvokeSubRule events are discarded, the invocation is carried forward with its events captured
    NO_RECURSION, // (Pre|Post)InvokeSubRule events are retained, the invocation is not resolved and the responsibility is given to the caller
    NO_RECURSION_PRE_ONLY, // PreInvokeSubRule events are retained, PostInvokeSubRule events are discarded, as above for invocation
}

enum class CaptureStackOverflowMode {
    KEEP, // Keep RaiseStackOverflow events, it's the responsibility of the caller to deal with them
    LITERAL_OR_KEEP, // Attempt to call the SO callback, if it returns a literal log an AppendLiteral event instead; otherwise log a RaiseStackOverflow event
    LITERAL_OR_THROW, // Attempt to call the SO callback, if it returns a literal log an AppendLiteral event instead; otherwise rethrow the exception
    THROW_ONLY // Attempt to call the SO callback, if it returns a literal discard it; otherwise rethrow the exception
}

class CaptureMode(
    val invocationMode: CaptureInvocationMode = CaptureInvocationMode.RECURSE_AND_KEEP,
    val stackOverflowMode: CaptureStackOverflowMode = CaptureStackOverflowMode.LITERAL_OR_THROW
)

sealed interface BuildingEvent

class AppendLiteral(val literal: String) : BuildingEvent

sealed class InvokeSubRule(val rule: InvokableRule) : BuildingEvent
class PreInvokeSubRule(rule: InvokableRule) : InvokeSubRule(rule)
class PostInvokeSubRule(rule: InvokableRule) : InvokeSubRule(rule)

class RaiseStackOverflow(val error: StackOverflowError?, val associatedLiteralGetter: () -> CharSequence?) : BuildingEvent {
    val associatedLiteral: CharSequence? = this.associatedLiteralGetter()
}

inline fun <reified E : BuildingEvent> BuildingEventList.filterOnlyOfType(): List<E> = this.filterOnlyOfType(E::class)
fun <E : BuildingEvent> BuildingEventList.filterOnlyOfType(type: KClass<E>): List<E> = this.mapNotNull { type.safeCast(it) }

fun BuildingEventList.filterAllAppends(): List<AppendLiteral> = this.filterOnlyOfType<AppendLiteral>()

fun BuildingEventList.concatenateAppends(separator: String = ""): String = this.filterAllAppends().joinToString(separator = separator) { it.literal }
