package io.github.koalaplot.core.util

import org.junit.Test
import kotlin.test.assertEquals

class UtilTest {
    @Test
    fun testMaximize() {
        val x = maximize(0.0, 10.0, 0.01) {
            it < 5.0
        }
        assertEquals(5.0, x, 5.0 * 0.01, "Result $x was not close to 5.0")
    }

    @Test
    fun testMaximizeWithInfinity() {
        val x = maximize(0.0, Double.POSITIVE_INFINITY, 0.01) {
            it < 5.0
        }
        assertEquals(5.0, x, 5.0 * 0.01, "Result $x was not close to 5.0")
    }

    @Test
    fun testMaximizeInverted() {
        val x = -maximize(-10.0, 0.0, 0.01) {
            it < -5.0
        }
        assertEquals(5.0, x, 5.0 * 0.01, "Result $x was not close to 5.0")
    }

    @Test
    fun testLineDistance1() {
        val p1 = Vector(0f, 0f)
        val p2 = Vector(10f, 0f)
        val p0 = Vector(0f, 5f)
        val r = lineDistance(p1, p2, p0)
        assertEquals(5f, r.first)
        assertEquals(Vector(0f, 0f), r.second)
    }

    @Test
    fun testLineDistance2() {
        val p1 = Vector(0f, 0f)
        val p2 = Vector(0f, 10f)
        val p0 = Vector(5f, 0f)
        val r = lineDistance(p1, p2, p0)
        assertEquals(5f, r.first)
        assertEquals(Vector(0f, 0f), r.second)
    }

    @Test
    fun testLineDistance3() {
        val p1 = Vector(0f, 0f)
        val p2 = Vector(10f, 10f)
        val p0 = Vector(0f, 10f)
        val r = lineDistance(p1, p2, p0)
        assertEquals(7.071068f, r.first, 1e-5f)
        assertEquals(Vector(5f, 5f), r.second)
    }

    internal fun assertEquals(v1: Vector, v2: Vector) {
        assertEquals(v1.values.size, v2.values.size, "Vectors are not same length")
        v1.values.forEachIndexed { index, v ->
            assertEquals(v, v2.values[index], 1e-6f, "Vector values at index $index not equal")
        }
    }
}
