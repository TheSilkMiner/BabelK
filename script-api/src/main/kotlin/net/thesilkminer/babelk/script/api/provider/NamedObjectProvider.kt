package net.thesilkminer.babelk.script.api.provider

import net.thesilkminer.babelk.script.api.NamedObject

interface NamedObjectProvider<out T : NamedObject> : Provider<T> {
    val name: String
}
