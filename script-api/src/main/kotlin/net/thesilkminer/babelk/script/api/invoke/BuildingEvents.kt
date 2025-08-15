@file:JvmName("BuildingEvents")

package net.thesilkminer.babelk.script.api.invoke

import kotlin.reflect.KClass
import kotlin.reflect.safeCast

typealias BuildingEventList = List<BuildingEvent>

enum class CaptureMode {
    RECURSE, // sub-rule events are captured too -- InvokeSubRule is present
    RECURSE_NO_SUBRULE, // sub-rule events are captured too -- InvokeSubRule is filtered out
    THIS_INVOCATION // sub-rules are skipped, only the event is logged -- the caller handles invoking sub-rules
}

sealed interface BuildingEvent

class AppendLiteral(val literal: String) : BuildingEvent
class InvokeSubRule(val rule: InvokableRule) : BuildingEvent

inline fun <reified E : BuildingEvent> BuildingEventList.filterOnlyOfType(): List<E> = this.filterOnlyOfType(E::class)
fun <E : BuildingEvent> BuildingEventList.filterOnlyOfType(type: KClass<E>): List<E> = this.mapNotNull { type.safeCast(it) }

fun BuildingEventList.filterAllAppends(): List<AppendLiteral> = this.filterOnlyOfType<AppendLiteral>()

fun BuildingEventList.concatenateAppends(separator: String = ""): String = this.filterAllAppends().joinToString(separator = separator) { it.literal }
