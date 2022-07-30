package io.github.koalaplot.core.util

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Constraints
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal const val DEG2RAD = PI / 180.0

internal const val DegreesFullCircle = 360.0f

/**
 * Polar to cartesian coordinates.
 */
internal fun pol2Cart(radius: Float, angleDeg: Float): Offset {
    val angleRad = angleDeg * DEG2RAD
    return Offset(cos(angleRad).toFloat(), sin(angleRad).toFloat()) * radius
}

/**
 * Computes the 2 possible angles, in radians, that correspond to the provided y coordinate
 * and radius.
 */
internal fun y2theta(y: Float, radius: Float): Pair<Float, Float> {
    val theta = asin(y / radius)
    return Pair(theta, (PI - theta).toFloat())
}

/**
 * Returns the edge-length of a square circumscribed by a circle with the provided [diameter].
 */
internal fun circumscribedSquareSize(diameter: Double): Double {
    return diameter / sqrt(2.0)
}

internal fun Path.moveTo(offset: Offset) {
    moveTo(offset.x, offset.y)
}

internal fun Path.lineTo(offset: Offset) {
    lineTo(offset.x, offset.y)
}

/**
 * Creates constraints with a fixed width and original height.
 */
@Stable
internal fun Constraints.fixedWidth(
    width: Int
): Constraints {
    require(width >= 0) {
        "width($width) must be >= 0"
    }
    return copy(minWidth = width, maxWidth = width)
}

/**
 * Creates constraints with a fixed height and original width.
 */
@Stable
internal fun Constraints.fixedHeight(
    height: Int
): Constraints {
    require(height >= 0) {
        "height($height) must be >= 0"
    }
    return copy(minHeight = height, maxHeight = height)
}
