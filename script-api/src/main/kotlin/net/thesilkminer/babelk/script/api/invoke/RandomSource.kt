package net.thesilkminer.babelk.script.api.invoke

interface RandomSource {
    fun nextBits(count: Int): Int

    fun nextBoolean(): Boolean

    fun nextInt(): Int
    fun nextIntInRange(begin: Int, end: Int): Int

    fun nextLong(): Long
    fun nextLongInRange(begin: Long, end: Long): Long

    fun nextFloat(): Float
    fun nextFloatInRange(begin: Float, end: Float): Float

    fun nextDouble(): Double
    fun nextDoubleInRange(begin: Double, end: Double): Double

    fun nextBytes(count: Int): ByteArray

    fun nextSeededRandomSource(): RandomSource
}
