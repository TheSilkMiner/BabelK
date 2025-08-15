package net.thesilkminer.babelk.script.api.invoke

interface RuleState {
    val ruleName: String?

    operator fun <T> get(key: RuleStateKey<T>): T?
    operator fun <T> set(key: RuleStateKey<T>, value: T)

    fun <T> getOrPut(key: RuleStateKey<T>, default: () -> T): T
    fun <T> update(key: RuleStateKey<T>, updater: (T?) -> T): T
    fun <T> updateIfPresent(key: RuleStateKey<T>, updater: (T) -> T): T?
}
