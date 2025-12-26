package io.github.koalaplot.core.util

import org.junit.Test
import kotlin.test.assertEquals

class RangeTest {
    internal fun <T> assertLerpEquals(
        range: ClosedRange<T>,
        expected: List<Pair<Double, T>>,
    ) where T : Comparable<T>, T : Number {
        val result = expected.map { it.first to range.lerp(it.first) }
        assertEquals(expected.toString(), result.toString())
    }

    @Test
    fun testLerpDoubleScales() {
        assertLerpEquals(
            range = 0.0..10.0,
            expected = listOf(
                +0.0 to 0.0,
                +1.0 to 10.0,
                +0.5 to 5.0,
                -1.0 to -10.0,
                +1.5 to 15.0,
            ),
        )
    }

    @Test
    fun testLerpDoubleOffsets() {
        assertLerpEquals(
            range = 10.0..11.0,
            expected = listOf(
                +0.0 to 10.0,
                +1.0 to 11.0,
                +0.5 to 10.5,
                -1.0 to 9.0,
                +1.5 to 11.5,
            ),
        )
    }

    @Test
    fun testLerpFloatScales() {
        assertLerpEquals(
            range = 0.0f..10.0f,
            expected = listOf(
                +0.0 to 0.0f,
                +1.0 to 10.0f,
                +0.5 to 5.0f,
                -1.0 to -10.0f,
                +1.5 to 15.0f,
            ),
        )
    }

    @Test
    fun testLerpFloatOffsets() {
        assertLerpEquals(
            range = 10.0f..11.0f,
            expected = listOf(
                +0.0 to 10.0f,
                +1.0 to 11.0f,
                +0.5 to 10.5f,
                -1.0 to 9.0f,
                +1.5 to 11.5f,
            ),
        )
    }

    @Test
    fun testLerpIntegerDoesFloor() {
        assertLerpEquals(
            range = 0..10,
            expected = listOf(
                +0.0 to 0,
                +0.99 to 9,
                +1.0 to 10,
                +0.19 to 1,
                +0.2 to 2,
                // negative does floor
                -0.2 to -2,
                -0.199 to -2,
                -0.21 to -3,
            ),
        )
    }

    @Test
    fun testLerpIntegerInverted() {
        assertLerpEquals(
            range = 10..0,
            expected = listOf(
                +0.0 to 10,
                +1.0 to 0,
                +0.99 to 0,
                +1.1 to -1,
                +0.19 to 8,
                +0.2 to 8,
                +0.21 to 7,
                -0.21 to 12,
                -0.2 to 12,
                -0.199 to 11,
            ),
        )
    }

    internal fun <T> assertNormalizeEquals(
        range: ClosedRange<T>,
        expected: List<Pair<T, Double>>,
    ) where T : Comparable<T>, T : Number {
        val result = expected.map { it.first to range.normalize(it.first) }
        assertEquals(expected.toString(), result.toString())
    }

    @Test
    fun testNormalizeInteger() {
        assertNormalizeEquals(
            range = 0..10,
            expected = listOf(
                0 to 0.0,
                10 to 1.0,
                2 to 0.2,
                -1 to -0.1,
                11 to 1.1,
            ),
        )
    }

    @Test
    fun testNormalizeDouble() {
        assertNormalizeEquals(
            range = -10.0..+10.0,
            expected = listOf(
                -10.0 to 0.0,
                +10.0 to 1.0,
                +0.0 to 0.5,
                +2.0 to 0.6,
            ),
        )
    }

    fun testNormalizeDoubleInverted() {
        assertNormalizeEquals(
            range = +10.0..-10.0,
            expected = listOf(
                -10.0 to 1.0,
                +10.0 to 0.0,
                +0.0 to 0.95,
                +2.0 to 0.4,
            ),
        )
    }
}
