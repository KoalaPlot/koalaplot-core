@file:Suppress("TooManyFunctions")

package io.github.koalaplot.core.util

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Constraints
import kotlin.jvm.JvmInline
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal const val DEG2RAD = PI / 180.0

internal const val DegreesFullCircle = 360.0f

/**
 * AngularValue is a sealed type hierarchy representing angular position around the unit circle.
 */
public sealed interface AngularValue

/**
 * An [AngularValue] in units of Degrees.
 */
@JvmInline
public value class Degrees(public val value: Double) : AngularValue {
    public override fun toString(): String {
        return value.toString()
    }
}

/**
 * An [AngularValue] in units of Radians.
 */
@JvmInline
public value class Radians(public val value: Double) : AngularValue {
    public override fun toString(): String {
        return value.toString()
    }
}

/**
 * Converts from [Degrees] to [Radians].
 */
public fun Degrees.toRadians(): Radians = Radians(value * (DEG2RAD))

/**
 * Converts from [Radians] to [Degrees].
 */
public fun Radians.toDegrees(): Degrees = Degrees(value / (DEG2RAD))

/**
 * Converts a Float to the [Radians] type.
 */
public fun Float.toRadians(): Radians = Radians(this.toDouble())

/**
 * Converts a Double to the [Radians] type.
 */
public fun Double.toRadians(): Radians = Radians(this)

/**
 * Converts a Float to the [Degrees] type.
 */
public fun Float.toDegrees(): Degrees = Degrees(this.toDouble())

/**
 * Converts a Double to the [Degrees] type.
 */
public fun Double.toDegrees(): Degrees = Degrees(this)

public inline val Float.rad: Radians get() = Radians(this.toDouble())
public inline val Double.rad: Radians get() = Radians(this)
public inline val Float.deg: Degrees get() = Degrees(this.toDouble())
public inline val Double.deg: Degrees get() = Degrees(this)
public inline val Int.deg: Degrees get() = Degrees(this.toDouble())

/**
 * Returns an [AngularValue] as [Radians].
 */
public fun AngularValue.toRadians(): Radians {
    return when (this) {
        is Radians -> this
        is Degrees -> toRadians()
    }
}

/**
 * Returns an [AngularValue] as [Degrees].
 */
public fun AngularValue.toDegrees(): Degrees {
    return when (this) {
        is Radians -> toDegrees()
        is Degrees -> this
    }
}

/**
 * Polar to cartesian coordinate transformation.
 */
internal fun polarToCartesian(radius: Float, angle: Degrees): Offset = polarToCartesian(radius, angle.toRadians())

/**
 * Polar to cartesian coordinate transformation.
 */
internal fun polarToCartesian(radius: Float, angle: Radians): Offset {
    return Offset((radius * cos(angle.value)).toFloat(), (radius * sin(angle.value)).toFloat())
}

internal fun polarToCartesian(radius: Float, angle: AngularValue): Offset {
    return polarToCartesian(radius, angle.toRadians())
}

internal fun cos(angle: AngularValue): Double = cos(angle.toRadians().value)
internal fun sin(angle: AngularValue): Double = sin(angle.toRadians().value)

internal data class PolarCoordinate(val radius: Float, val angle: AngularValue)

/**
 * Cartesian to polar coordinate transformation.
 */
internal fun cartesianToPolar(offset: Offset): PolarCoordinate {
    val distance = offset.getDistance()
    return if (distance == 0f) {
        PolarCoordinate(0f, Radians(0.0))
    } else {
        // acos returns an angle between 0 and PI
        var theta = acos(offset.x / distance)
        if (offset.y < 0) {
            theta = -theta
        }
        PolarCoordinate(offset.getDistance(), Radians(theta.toDouble()))
    }
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
