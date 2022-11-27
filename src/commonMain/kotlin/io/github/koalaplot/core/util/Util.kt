package io.github.koalaplot.core.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

/**
 * Converts the float receiver as a string with [precision] number of digits after the decimal
 * (rounded) (e.g. 35.72 with precision = 1 will be 35.7, 35.78 with precision = 2 will be 35.80)
 */
public fun Float.toString(precision: Int): String {
    return this.toDouble().toString(precision)
}

/**
 * Converts the number to a string with [precision] number of digits after the decimal (rounded)
 * (e.g. 35.72 with precision = 1 will be 35.7, 35.78 with precision = 2 will be 35.80)
 */
public fun Double.toString(precision: Int): String {
    val leftShifted = (round(abs(this) * 10.0.pow(precision))).toInt()
    val s = StringBuilder(leftShifted.toString())

    // left-pad with 0's to ensure enough digits
    (1..(precision + 1 - s.length)).forEach { _ ->
        s.insert(0, "0")
    }

    // insert decimal point
    if (precision != 0) s.insert(s.lastIndex - (precision - 1), ".")

    // (re)insert negative sign
    if (this < 0) {
        s.insert(0, "-")
    }

    return s.toString()
}

/**
 * Seeks to find the maximum value, within the range of [min] and [max] and with an accuracy
 * of [tolerance], that results in a true result from the [eval] function.
 * [eval] must be a monotonically increasing function.
 * [min] must be a value that causes [eval] to return true, and max must be greater than [min].
 * [max] may be +infinity, but otherwise [min] and [max] must be numbers.
 */
internal fun maximize(
    min: Double,
    max: Double,
    tolerance: Double = 0.01,
    eval: (Double) -> Boolean
): Double {
    require(min <= max)
    require(tolerance > 0)
    require(min.isFinite())
    require(!max.isNaN())

    return maximizeUnguarded(min, max, tolerance, eval)
}

/**
 * This is the implementation of the recursive maximize function without the parameter checks
 * for performance purposes.
 */
private fun maximizeUnguarded(
    min: Double,
    max: Double,
    tolerance: Double = 0.01,
    eval: (Double) -> Boolean
): Double {
    if (abs((max - min) / min) < tolerance) {
        return min
    }

    val test = if (max.isInfinite()) {
        if (min == 0.0) {
            1.0
        } else {
            min * 2.0
        }
    } else {
        min + (max - min) / 2.0
    }

    return if (eval(test)) {
        maximizeUnguarded(test, max, tolerance, eval)
    } else {
        maximizeUnguarded(min, test, tolerance, eval)
    }
}

@RequiresOptIn(
    "This KoalaPlot API is experimental and is likely to change or to be removed in" +
        " the future."
)
public annotation class ExperimentalKoalaPlotApi

/**
 * Modifier to rotate the target Composable vertically.
 *
 * Using Modifier.rotate is arguably broken because it doesn't affect the measurements of the
 * rotated element.
 * See https://stackoverflow.com/questions/70057396/how-to-show-vertical-text-with-proper-size-layout-in-jetpack-compose
 */
public fun Modifier.rotateVertically(rotation: VerticalRotation): Modifier = then(
    object : LayoutModifier {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            return layout(placeable.height, placeable.width) {
                placeable.place(
                    x = -(placeable.width / 2 - placeable.height / 2),
                    y = -(placeable.height / 2 - placeable.width / 2)
                )
            }
        }

        override fun IntrinsicMeasureScope.minIntrinsicHeight(
            measurable: IntrinsicMeasurable,
            width: Int
        ): Int {
            return measurable.maxIntrinsicWidth(width)
        }

        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
            measurable: IntrinsicMeasurable,
            width: Int
        ): Int {
            return measurable.maxIntrinsicWidth(width)
        }

        override fun IntrinsicMeasureScope.minIntrinsicWidth(
            measurable: IntrinsicMeasurable,
            height: Int
        ): Int {
            return measurable.minIntrinsicHeight(height)
        }

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
            measurable: IntrinsicMeasurable,
            height: Int
        ): Int {
            return measurable.maxIntrinsicHeight(height)
        }
    }
).then(rotate(rotation.value))

@Suppress("MagicNumber")
public enum class VerticalRotation(internal val value: Float) {
    CLOCKWISE(90f), COUNTER_CLOCKWISE(270f)
}

internal fun min(vararg values: Float): Float {
    var m = Float.POSITIVE_INFINITY
    values.forEach {
        m = kotlin.math.min(m, it)
    }
    return m
}

internal fun max(vararg values: Float): Float {
    var m = Float.NEGATIVE_INFINITY
    values.forEach {
        m = kotlin.math.max(m, it)
    }
    return m
}
