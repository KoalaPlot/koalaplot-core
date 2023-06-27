@file:Suppress("TooManyFunctions")

package io.github.koalaplot.core.util

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility functions related to vector type operations.
 */

internal operator fun IntSize.minus(other: IntSize): IntSize {
    return IntSize(this.width - other.width, this.height - other.height)
}

internal fun IntSize.length(): Float {
    return sqrt((width * width + height * height).toDouble()).toFloat()
}

internal fun IntOffset.toVector(): Vector = Vector(x.toFloat(), y.toFloat())

internal fun Vector.toIntOffset(): IntOffset {
    require(values.size == 2) { "Only 2d Vectors can be converted to an IntOffset" }
    return IntOffset(values[0].toInt(), values[1].toInt())
}

internal data class Vector(val values: List<Float>) {
    /**
     * Creates a new 2d vector.
     */
    constructor(x: Float, y: Float) : this(listOf(x, y))

    internal operator fun get(i: Int) = values[i]

    // vector * scalar
    internal operator fun times(scalar: Float): Vector = Vector(values.map { it * scalar })

    internal operator fun div(f: Float): Vector {
        return times(1 / f)
    }

    /**
     * Dot product of 2 vectors.
     */
    internal operator fun times(vector: Vector): Float {
        require(values.size == vector.values.size) { "Dot product requires that both vectors are the same length" }

        return values.foldIndexed(0f) { index, acc, t ->
            acc + t * vector.values[index]
        }
    }

    internal operator fun plus(vector: Vector): Vector {
        require(values.size == vector.values.size) { "Addition requires that both vectors are the same length" }

        return Vector(
            values.mapIndexed { index, i ->
                i + vector.values[index]
            }
        )
    }

    internal operator fun minus(vector: Vector): Vector {
        require(values.size == vector.values.size) { "Subtraction requires that both vectors are the same length" }
        return Vector(
            values.mapIndexed { index, i ->
                i - vector.values[index]
            }
        )
    }

    internal fun norm(): Float {
        return sqrt(
            values.fold(0f) { acc, fl ->
                acc + fl * fl
            }
        )
    }
}

// scalar * vector
internal operator fun Float.times(vector: Vector): Vector = vector.times(this)

internal const val Deg2Rad = PI / 180.0f

/**
 * Rotates the provided point through an [angle] provided in degrees where positive angles are counterclockwise.
 */
internal fun IntOffset.rotate(angle: Int): IntOffset {
    val angleRad = angle * Deg2Rad
    return IntOffset(
        (x * cos(angleRad) - y * sin(angleRad)).roundToInt(),
        (x * sin(angleRad) + y * cos(angleRad)).roundToInt()
    )
}

/**
 * Rotates the 2-d vector through an [angle] provided in degrees where positive angles are counterclockwise.
 */
internal fun Vector.rotate(angle: Float): Vector {
    require(values.size == 2) { "Must be a 2-d vector to rotate" }

    val angleRad = angle * Deg2Rad
    return Vector(
        (values[0] * cos(angleRad) - values[1] * sin(angleRad)).toFloat(),
        (values[0] * sin(angleRad) + values[1] * cos(angleRad)).toFloat()
    )
}

/**
 * Computes the unit vector representing a line between the two points [p1] and [p2].
 */
internal fun line(p1: Vector, p2: Vector): Vector = (p2 - p1) / (p2 - p1).norm()
