@file:JvmName("Callbacks")

package net.thesilkminer.babelk.script.host.interop

import kotlin.reflect.KClass

typealias LoadingScriptCollectionData = Collection<LoadingScriptData>

interface LoadingScriptData {
    val grammarName: String
    val allClassNames: Collection<String>
    fun classDataFor(name: String): ByteArray?
}

typealias ClassloadingCallback = (grammar: String, className: String) -> KClass<*>

interface LoadingCallbacks {
    fun setUpClassLoadingFor(scripts: LoadingScriptCollectionData): ClassloadingCallback
}
