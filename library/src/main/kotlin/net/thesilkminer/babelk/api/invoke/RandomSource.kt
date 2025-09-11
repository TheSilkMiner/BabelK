package net.thesilkminer.babelk.api.invoke

import kotlin.random.Random

interface RandomSource {
    companion object Default : RandomSource {
        private val random = RandomSource(Random.Default)

        override fun nextBits(bits: Int): Int = this.random.nextBits(bits)
        override fun nextBoolean(): Boolean = this.random.nextBoolean()
        override fun nextInt(): Int = this.random.nextInt()
        override fun nextIntInRange(beginning: Int, end: Int): Int = this.random.nextIntInRange(beginning, end)
        override fun nextLong(): Long = this.random.nextLong()
        override fun nextLongInRange(beginning: Long, end: Long): Long = this.random.nextLongInRange(beginning, end)
        override fun nextDouble(): Double = this.random.nextDouble()
        override fun nextDoubleInRange(beginning: Double, end: Double): Double = this.random.nextDoubleInRange(beginning, end)
        override fun nextFloat(): Float = this.random.nextFloat()
        override fun nextFloatInRange(beginning: Float, end: Float): Float = this.random.nextFloatInRange(beginning, end)
        override fun nextBytes(count: Int): ByteArray = this.random.nextBytes(count)

        override fun toString(): String = this.random.toString()
    }

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
    fun nextSeededRandomSource(): RandomSource = RandomSource(this.nextSeededRandom())
}
