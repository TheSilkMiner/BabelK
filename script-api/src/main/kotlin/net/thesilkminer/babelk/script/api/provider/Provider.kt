package net.thesilkminer.babelk.script.api.provider

interface Provider<out T> {
    fun get(): T
}