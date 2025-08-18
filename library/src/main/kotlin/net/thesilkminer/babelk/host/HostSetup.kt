@file:JvmName("HostSetup")
@file:OptIn(ExperimentalAtomicApi::class, ExperimentalContracts::class)

package net.thesilkminer.babelk.host

import net.thesilkminer.babelk.script.host.LogCreator
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.withLock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private val hostSetup: AtomicBoolean = AtomicBoolean(false)
private val lock: Lock = ReentrantLock()

internal inline fun <R> setupHostAndThen(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    setupHost()
    return block()
}

private fun setupHost() {
    if (hostSetup.load()) {
        return
    }

    lock.withLock {
        if (hostSetup.load()) {
            return
        }

        LogCreator.instance = HostLogCreator()

        hostSetup.store(true)
    }
}
