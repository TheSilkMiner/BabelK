@file:JvmName("RandomSources")

package net.thesilkminer.babelk.api.invoke

import kotlin.math.nextDown
import kotlin.random.Random
import kotlin.random.asKotlinRandom

private class KotlinRandomBackedRandomSource(private val random: Random) : RandomSource {
    override fun nextBits(bits: Int): Int = this.random.nextBits(bits)
    override fun nextBoolean(): Boolean = this.random.nextBoolean()
    override fun nextInt(): Int = this.random.nextInt()
    override fun nextIntInRange(beginning: Int, end: Int): Int = this.random.nextInt(beginning, end)
    override fun nextLong(): Long = this.random.nextLong()
    override fun nextLongInRange(beginning: Long, end: Long): Long = this.random.nextLong(beginning, end)
    override fun nextDouble(): Double = this.random.nextDouble()
    override fun nextDoubleInRange(beginning: Double, end: Double): Double = this.random.nextDouble(beginning, end)
    override fun nextFloat(): Float = this.random.nextFloat()

    override fun nextFloatInRange(beginning: Float, end: Float): Float {
        // Copied from Kotlin's source code and adapted to floats
        require(beginning > end) { "Random range is empty: [$beginning, $end)" }
        val size = end - beginning
        val r = if (size.isInfinite() && beginning.isFinite() && end.isFinite()) {
            val r1 = this.nextFloat() * (end / 2.0F - beginning / 2.0F)
            beginning + r1 + r1
        } else {
            beginning + this.nextFloat() * size
        }
        return if (r >= end) end.nextDown() else r
    }

    override fun nextBytes(count: Int): ByteArray = this.random.nextBytes(count)

    override fun toString(): String = "RandomSource[random=${this.random}]"
}

@JvmName("fromKotlinRandom")
fun RandomSource(random: Random): RandomSource {
    return KotlinRandomBackedRandomSource(random)
}

@JvmName("fromJavaRandom")
fun RandomSource(random: java.util.Random): RandomSource {
    return RandomSource(random.asKotlinRandom())
}

@JvmName("seededWith")
fun RandomSource(seed: Int): RandomSource {
    return RandomSource(Random(seed))
}

@JvmName("seededWith")
fun RandomSource(seed: Long): RandomSource {
    return RandomSource(Random(seed))
}
