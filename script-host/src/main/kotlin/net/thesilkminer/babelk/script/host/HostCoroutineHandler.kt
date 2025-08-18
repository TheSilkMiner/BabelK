@file:JvmName("HostCoroutineHandler")

package net.thesilkminer.babelk.script.host

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

internal fun <R> withinCoroutine(dispatcher: CoroutineDispatcher = Dispatchers.Default, block: suspend () -> R): R {
    return runBlocking {
        withContext(dispatcher) {
            block()
        }
    }
}
