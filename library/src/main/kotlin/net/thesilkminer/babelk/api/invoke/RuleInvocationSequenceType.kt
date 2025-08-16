package net.thesilkminer.babelk.api.invoke

enum class RuleInvocationSequenceType {
    LIGHTWEIGHT, // Supports next() and next(Int)
    RETAINING, // Supports next(), next(Int), and nth(Int) if n <= current
    RANDOM_ACCESS // Supports next(), next(Int), and nth(Int) for any n
}
