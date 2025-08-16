package net.thesilkminer.babelk.api.invoke

import kotlin.random.Random

interface RandomSource {
    fun nextBits(bits: Int): Int

    fun nextBoolean(): Boolean

    fun nextInt(): Int
    fun nextIntInRange(beginning: Int, end: Int): Int

    fun nextLong(): Long
    fun nextLongInRange(beginning: Long, end: Long): Long

    fun nextDouble(): Double
    fun nextDoubleInRange(beginning: Double, end: Double): Double

    fun nextFloat(): Float
    fun nextFloatInRange(beginning: Float, end: Float): Float

    fun nextBytes(count: Int): ByteArray

    fun nextSeededRandom(): Random = Random(this.nextLong())
}
