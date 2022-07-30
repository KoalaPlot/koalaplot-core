package io.github.koalaplot.core.util

import androidx.compose.ui.unit.IntSize
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

// vector * scalar
internal operator fun List<Float>.times(scalar: Float): List<Float> = this.map { it * scalar }

// scalar * vector
internal operator fun Float.times(vector: List<Float>): List<Float> = vector.times(this)
