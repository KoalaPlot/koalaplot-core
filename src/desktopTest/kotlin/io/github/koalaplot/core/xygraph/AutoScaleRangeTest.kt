package io.github.koalaplot.core.xygraph

import junit.framework.TestCase.assertEquals
import org.junit.Test

class AutoScaleRangeTest {
    // First pair is the input min/max of the data list, second pair is the expected output of autoScaleRange()
    private val floatTestConditions: List<Pair<Pair<Float, Float>, Pair<Float, Float>>> = listOf(
        (0f to 50f) to (0f to 50f),
        (0f to 73f) to (0f to 80f),
        (-5f to 106f) to (-100f to 200f),
        (1024f to 4096f) to (1000f to 5000f),
        (15f to 123f) to (0f to 200f),
        (1.5E5f to 1.2E6f) to (0f to 2E6f),
        (-523f to 126f) to (-600f to 200f),
        (-12345f to -5076f) to (-13000f to -5000f),
        (0.1f to 0.67f) to (0.1f to 0.7f),
        (-1024f to 1024f) to (-2000f to 2000f),
        (-1.2E6f to -1.5E5f) to (-2E6f to 0f),
        (-0.67f to -0.1f) to (-0.7f to -0.1f),
        (-0.67f to -0.15f) to (-0.7f to -0.1f),
        (-0.67f to -0.05f) to (-0.7f to 0f),
        (0.05f to 0.57f) to (0f to 0.6f),
        (1f to 1f) to (0.5f to 2f),
        (0f to 0f) to (0.0f to 1f)
    )

    private val intTestConditions: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> = listOf(
        (0 to 50) to (0 to 50),
        (0 to 73) to (0 to 80),
        (-5 to 106) to (-100 to 200),
        (1024 to 4096) to (1000 to 5000),
        (15 to 123) to (0 to 200),
        (150000 to 1200000) to (0 to 2000000),
        (-523 to 126) to (-600 to 200),
        (-12345 to -5076) to (-13000 to -5000),
        (-1024 to 1024) to (-2000 to 2000),
        (-1200000 to -150000) to (-2000000 to 0),
        (1 to 1) to (0 to 2),
        (0 to 0) to (0 to 1)
    )

    @Test
    fun testWithFloat() {
        floatTestConditions.forEach {
            val range = listOf(it.first.first, it.first.second).autoScaleRange()
            assertEquals(it.second.first, range.start, (it.first.second - it.first.first) * 0.001f)
            assertEquals(it.second.second, range.endInclusive, (it.first.second - it.first.first) * 0.001f)
        }
    }

    @Test
    fun testWithInt() {
        intTestConditions.forEach {
            val range = listOf(it.first.first, it.first.second).autoScaleRange()
            assertEquals(it.second.first, range.first)
            assertEquals(it.second.second, range.last)
        }
    }

    @Test
    fun testWithEmptyList() {
        assertEquals(0f..1f, listOf<Float>().autoScaleRange())
        assertEquals(0f..1f, listOf<Point<Float, Float>>().autoScaleXRange())
        assertEquals(0f..1f, listOf<Point<Float, Float>>().autoScaleYRange())

        assertEquals(0.0..1.0, listOf<Double>().autoScaleRange())
        assertEquals(0.0..1.0, listOf<Point<Double, Double>>().autoScaleXRange())
        assertEquals(0.0..1.0, listOf<Point<Double, Double>>().autoScaleYRange())

        assertEquals(0..1, listOf<Int>().autoScaleRange())
        assertEquals(0..1, listOf<Point<Int, Int>>().autoScaleXRange())
        assertEquals(0..1, listOf<Point<Int, Int>>().autoScaleYRange())

        assertEquals(0L..1L, listOf<Long>().autoScaleRange())
        assertEquals(0L..1L, listOf<Point<Long, Long>>().autoScaleXRange())
        assertEquals(0L..1L, listOf<Point<Long, Long>>().autoScaleYRange())
    }
}
