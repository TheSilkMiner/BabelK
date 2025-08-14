@file:JvmName("Providers")

package net.thesilkminer.babelk.script.api.provider

import net.thesilkminer.babelk.script.api.NamedObject

private class SimpleProvider<T>(private val lambda: () -> T) : Provider<T> {
    override fun get(): T = this.lambda()
}

private class SimpleMemoizedProvider<T>(creator: () -> T) : Provider<T> {
    private val memoized by lazy(creator)

    override fun get(): T = this.memoized
}

private class SimpleWithNameNamedObjectProvider<T : NamedObject>(override val name: String, private val lambda: () -> T) : NamedObjectProvider<T> {
    override fun get(): T = this.lambda()
}

private class SimpleWithNameMemoizedNamedObjectProvider<T : NamedObject>(override val name: String, creator: () -> T) : NamedObjectProvider<T> {
    private val memoized by lazy(creator)
    override fun get(): T = this.memoized
}

private class SimpleNamedMemoizedObjectProvider<T : NamedObject>(creator: () -> T) : NamedObjectProvider<T> {
    private val memoized by lazy(creator)
    override val name: String get() = this.memoized.name
    override fun get(): T = this.memoized
}

fun <T> Provider(memoize: Boolean = true, creator: () -> T): Provider<T> {
    return if (memoize) SimpleMemoizedProvider(creator) else SimpleProvider(creator)
}

fun <T : NamedObject> NamedObjectProvider(creator: () -> T): NamedObjectProvider<T> {
    return SimpleNamedMemoizedObjectProvider(creator)
}

fun <T : NamedObject> NamedObjectProvider(name: String, memoize: Boolean = true, creator: () -> T): NamedObjectProvider<T> {
    return if (memoize) SimpleWithNameMemoizedNamedObjectProvider(name, creator) else SimpleWithNameNamedObjectProvider(name, creator)
}
