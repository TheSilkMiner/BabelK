@file:JvmName("HostLoader")

package net.thesilkminer.babelk.script.host.flow.loading

import net.thesilkminer.babelk.script.host.Log
import net.thesilkminer.babelk.script.host.flow.EvaluableScript
import net.thesilkminer.babelk.script.host.flow.LoadableScript
import net.thesilkminer.babelk.script.host.interop.LoadingCallbacks
import net.thesilkminer.babelk.script.host.interop.LoadingScriptData

private val log = Log {}

internal fun Collection<LoadableScript>.load(callbacks: LoadingCallbacks): Collection<EvaluableScript> {
    log.info { "Setting up classloading for ${this.count()} compiled scripts using $callbacks" }

    val hook = this.obtainHook(callbacks)
    return this.map { EvaluableHookedLoadableScript(it, hook) }
}

private fun Collection<LoadableScript>.obtainHook(callbacks: LoadingCallbacks): ClassLoaderHook {
    val loadableCollection = this.map(LoadableScript::prepareScriptForClassLoading)
    val callback = callbacks.setUpClassLoadingFor(loadableCollection)
    return ClassLoaderHook(callback).also { log.debug { "Using hook '$it'" } }
}

private fun LoadableScript.prepareScriptForClassLoading(): LoadingScriptData = LoadableScriptCallbackData(this)
