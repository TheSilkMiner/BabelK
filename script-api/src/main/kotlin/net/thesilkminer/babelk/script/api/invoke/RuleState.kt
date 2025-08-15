package net.thesilkminer.babelk.script.api.invoke

interface RuleState {
    val ruleName: String?

    operator fun <T : Any> get(key: RuleStateKey<T>): T?
    operator fun <T : Any> set(key: RuleStateKey<T>, value: T)

    fun <T : Any> getOrPut(key: RuleStateKey<T>, default: () -> T): T
    fun <T : Any> update(key: RuleStateKey<T>, updater: (T?) -> T): T
    fun <T : Any> updateIfPresent(key: RuleStateKey<T>, updater: (T) -> T): T?
}
