@file:JvmName("HostSetup")
@file:OptIn(ExperimentalAtomicApi::class, ExperimentalContracts::class)

package net.thesilkminer.babelk.host

import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal fun <R> setupHostAndThen(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    setupHost()
    return block()
}

// Let's keep it just in case we'll need to do something else in the future
private fun setupHost() = Unit
