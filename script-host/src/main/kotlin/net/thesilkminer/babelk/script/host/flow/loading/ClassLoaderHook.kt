package net.thesilkminer.babelk.script.host.flow.loading

import net.thesilkminer.babelk.script.host.interop.ClassloadingCallback
import kotlin.reflect.KClass

internal class ClassLoaderHook(private val lambda: ClassloadingCallback) {
    operator fun invoke(grammarName: String, className: String): KClass<*> = this.lambda(grammarName, className)
    override fun toString(): String = "ClassLoaderHook with lambda ${this.lambda}"
}
