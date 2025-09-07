package io.github.koalaplot.core.xygraph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign

/**
 * An [AxisModel] that uses Double values and is linear.
 *
 * @param range  The minimum to maximum values allowed to be represented on this Axis. Zoom and
 * scroll modifications may not exceed this range.
 * @param minViewExtent Specifies the minimum allowed range after zooming. Must
 * be greater than 0 and less than the difference between the start and end of [range].
 * @param maxViewExtent Specifies the maximum allowed viewable range. Must be greater than 0, greater
 * than or equal to minViewExtent, and less than or equal to the difference between the start and end of [range].
 * @param minimumMajorTickIncrement The minimum value between adjacent major ticks. Must be less than or equal to
 * the extent of the range.
 * @param minimumMajorTickSpacing Specifies the minimum physical spacing for major ticks, in
 * Dp units. Must be greater than 0.
 * @param minorTickCount The number of minor ticks per major tick interval.
 */
public class DoubleLinearAxisModel(
    public override val range: ClosedFloatingPointRange<Double>,
    private val minViewExtent: Double = (range.endInclusive - range.start) * ZoomRangeLimitDefault,
    private val maxViewExtent: Double = ((range.endInclusive - range.start)),
    private val minimumMajorTickIncrement: Double =
        (range.endInclusive - range.start) * MinimumMajorTickIncrementDefault,
    override val minimumMajorTickSpacing: Dp = 50.dp,
    private val minorTickCount: Int = 4,
    inverted: Boolean = false,
) : ContinuousLinearAxisModel<Double> {
    init {
        require(range.endInclusive > range.start) {
            "Axis range end (${range.endInclusive}) must be greater than start (${range.start})"
        }
        require(minimumMajorTickSpacing > 0.dp) { "Minimum major tick spacing must be greater than 0 dp" }
        require(minViewExtent > 0.0) {
            "minViewExtent must be greater than 0"
        }
        require(maxViewExtent > 0.0 && maxViewExtent >= minViewExtent) {
            "maxViewExtent must be greater than 0 and greater than or equal to minViewExtent"
        }
        require(minViewExtent <= range.endInclusive - range.start) {
            "minViewExtent must be less than or equal to range"
        }
        require(maxViewExtent <= range.endInclusive - range.start) {
            "maxViewExtent must be less than or equal to range"
        }
        require(minimumMajorTickIncrement <= range.endInclusive - range.start) {
            "minimumMajorTickIncrement must be less than or equal to the axis range"
        }
    }

    // Function used to compute point offsets. Will be modified from the default if inverted is true.
    private var offsetComputer = { point: Double ->
        (
            (point - currentRange.value.start) / (currentRange.value.endInclusive - currentRange.value.start)
            ).toFloat()
    }

    // Function used to compute the inverse of offsetComputer. Set as a variable to avoid an
    // additional check for inverted on every execution.
    private var offsetToValueFunction: (Float) -> Double = { offset ->
        require(offset in 0f..1f) { "Offset must be within [0, 1]" }
        currentRange.value.start + offset * (currentRange.value.endInclusive - currentRange.value.start)
    }

    init {
        if (inverted) {
            offsetComputer = { point: Double ->
                (
                    (currentRange.value.endInclusive - point) /
                        (currentRange.value.endInclusive - currentRange.value.start)
                    ).toFloat()
            }

            offsetToValueFunction = { offset ->
                require(offset in 0f..1f) { "Offset must be within [0, 1]" }
                currentRange.value.endInclusive - offset * (currentRange.value.endInclusive - currentRange.value.start)
            }
        }
    }

    private var currentRange = mutableStateOf(range.start..(range.start + maxViewExtent))

    public override val viewRange: State<ClosedRange<Double>> = currentRange

    override fun computeOffset(point: Double): Float = offsetComputer(point)

    override fun offsetToValue(offset: Float): Double = offsetToValueFunction(offset)

    private val tickCalculator =
        DoubleTickCalculator(minimumMajorTickSpacing, currentRange, minimumMajorTickIncrement, minorTickCount, inverted)

    override fun computeTickValues(axisLength: Dp): TickValues<Double> = tickCalculator.computeTickValues(axisLength)

    override fun zoom(zoomFactor: Float, pivot: Float) {
        if (zoomFactor == 1f) return

        require(zoomFactor > 0) { "Zoom amount must be greater than 0" }
        require(pivot in 0.0..1.0) { "Zoom pivot must be between 0 and 1: $pivot" }

        if (zoomFactor > 1f && currentRange.value.endInclusive - currentRange.value.start <= minViewExtent) {
            // Can't zoom in more
        } else if (zoomFactor < 1f && currentRange.value.endInclusive - currentRange.value.start >= maxViewExtent) {
            // Can't zoom out more
        } else {
            // convert pivot to axis range space
            val pivotAxisScale =
                (currentRange.value.start) + (currentRange.value.endInclusive - currentRange.value.start) * pivot

            val newLow = (pivotAxisScale - (pivotAxisScale - currentRange.value.start) / zoomFactor)
            val newHi = (pivotAxisScale + (currentRange.value.endInclusive - pivotAxisScale) / zoomFactor)

            setViewRange(newLow..newHi)
        }
    }

    override fun pan(amount: Float): Boolean {
        // convert pan amount to axis range space
        val panAxisScale = (currentRange.value.endInclusive - currentRange.value.start) * amount

        // Limit pan amount to not exceed bounds of range
        val panLimitEnd = min(panAxisScale, range.endInclusive - currentRange.value.endInclusive)
        val panLimited = max(panLimitEnd, range.start - currentRange.value.start)

        val newLow = (currentRange.value.start + panLimited)
        val newHi = (currentRange.value.endInclusive + panLimited)

        val result = currentRange.value != newLow..newHi

        currentRange.value = newLow..newHi
        return result
    }

    override fun setViewRange(newRange: ClosedRange<Double>) {
        val newHi = newRange.endInclusive.coerceIn(range)
        val newLow = newRange.start.coerceIn(range)

        if (newHi - newLow < minViewExtent) {
            val delta = (minViewExtent - (newHi - newLow)) / 2
            currentRange.value = (newLow - delta)..(newHi + delta)
            if (currentRange.value.start < range.start) {
                currentRange.value = range.start..(range.start + minViewExtent)
            } else if (currentRange.value.endInclusive > range.endInclusive) {
                currentRange.value = (range.endInclusive - minViewExtent)..range.endInclusive
            }
        } else if (newHi - newLow > maxViewExtent) {
            val delta = (newHi - newLow - maxViewExtent) / 2
            currentRange.value = (newLow + delta)..(newHi - delta)
            if (currentRange.value.start < range.start) {
                currentRange.value = range.start..(range.start + maxViewExtent)
            } else if (currentRange.value.endInclusive > range.endInclusive) {
                currentRange.value = (range.endInclusive - maxViewExtent)..range.endInclusive
            }
        } else {
            currentRange.value = newLow..newHi
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DoubleLinearAxisModel

        if (range != other.range) return false
        if (minViewExtent != other.minViewExtent) return false
        if (minimumMajorTickIncrement != other.minimumMajorTickIncrement) return false
        if (minimumMajorTickSpacing != other.minimumMajorTickSpacing) return false
        return minorTickCount == other.minorTickCount
    }

    override fun hashCode(): Int {
        var result = range.hashCode()
        result = 31 * result + minViewExtent.hashCode()
        result = 31 * result + minimumMajorTickIncrement.hashCode()
        result = 31 * result + minimumMajorTickSpacing.hashCode()
        result = 31 * result + minorTickCount
        return result
    }
}

private class DoubleTickCalculator(
    private val minimumMajorTickSpacing: Dp,
    private val currentRange: MutableState<ClosedFloatingPointRange<Double>>,
    private val minimumMajorTickIncrement: Double,
    private val minorTickCount: Int,
    private val inverted: Boolean
) {
    fun computeTickValues(axisLength: Dp): TickValues<Double> {
        val minTickSpacing = if (axisLength == 0.dp) {
            1f
        } else {
            (minimumMajorTickSpacing / axisLength).coerceIn(0f..1f)
        }
        val majorTickValues = computeMajorTickValues(minTickSpacing)
        val minorTickValues = computeMinorTickValues(
            majorTickValues,
            computeMajorTickSpacing(minTickSpacing)
        )
        return object : TickValues<Double> {
            override val majorTickValues = if (inverted) majorTickValues.reversed() else majorTickValues
            override val minorTickValues = if (inverted) minorTickValues.reversed() else minorTickValues
        }
    }

    fun computeMajorTickSpacing(minTickSpacing: Float): Double {
        require(minTickSpacing > 0 && minTickSpacing <= 1) {
            "Minimum tick spacing must be greater than 0 and less than or equal to 1"
        }
        val length = currentRange.value.endInclusive - currentRange.value.start
        val magnitude = 10.0.pow(floor(log10(length)))
        val scaledTickRatios = TickRatios.map { it * magnitude }

        // Test scaledTickRatios and pick the first that produces a distance greater than minTickSpacing
        // and an increment greater than minimumMajorTickIncrement
        val tickSpacing = scaledTickRatios.find {
            it / length >= minTickSpacing && it >= minimumMajorTickIncrement
        } ?: minimumMajorTickIncrement

        return tickSpacing
    }

    /**
     * Computes major tick values based on a minimum tick spacing that is a
     * fraction of the overall axis length.
     *
     * @param minTickSpacing minimum tick spacing, must be greater than 0 and less than or equal to 1.
     */
    fun computeMajorTickValues(minTickSpacing: Float): List<Double> {
        val tickSpacing = computeMajorTickSpacing(minTickSpacing)

        return buildList {
            if (tickSpacing > 0) {
                var tickCount = floor(currentRange.value.start / tickSpacing)
                do {
                    val lastTick = tickCount * tickSpacing
                    if (lastTick in currentRange.value) {
                        add(lastTick)
                    }
                    tickCount++
                } while (lastTick < currentRange.value.endInclusive)
            }
        }
    }

    fun computeMinorTickValues(
        majorTickValues: List<Double>,
        majorTickSpacing: Double,
    ): List<Double> = buildList {
        if (minorTickCount > 0 && majorTickValues.isNotEmpty()) {
            val minorIncrement = majorTickSpacing / (minorTickCount + 1)

            // Create ticks between first and last major ticks
            for (major in 0 until majorTickValues.lastIndex) {
                val majorTick1 = majorTickValues[major]

                for (i in 1..minorTickCount) {
                    add(majorTick1 + minorIncrement * i)
                }
            }

            // create ticks after last major tick, if still space in the range
            var i = 1
            do {
                val nextTick = majorTickValues.last() + minorIncrement * i
                if (nextTick in currentRange.value) {
                    add(nextTick)
                }
                i++
            } while (nextTick in currentRange.value)

            // create ticks before first major tick. if still space in the range
            i = 1
            do {
                val nextTick = majorTickValues.first() - minorIncrement * i
                if (nextTick in currentRange.value) {
                    add(nextTick)
                }
                i++
            } while (nextTick in currentRange.value)
        }
    }
}

/**
 * Create and remember a DoubleLinearAxisModel. See [DoubleLinearAxisModel] for parameter descriptions.
 */
@Composable
public fun rememberDoubleLinearAxisModel(
    range: ClosedFloatingPointRange<Double>,
    minViewExtent: Double = (range.endInclusive - range.start) * ZoomRangeLimitDefault,
    maxViewExtent: Double = ((range.endInclusive - range.start)),
    minimumMajorTickIncrement: Double = (range.endInclusive - range.start) * MinimumMajorTickIncrementDefault,
    minimumMajorTickSpacing: Dp = 50.dp,
    minorTickCount: Int = 4,
): DoubleLinearAxisModel = remember(
    range,
    minViewExtent,
    minimumMajorTickIncrement,
    minimumMajorTickSpacing,
    minorTickCount,
) {
    DoubleLinearAxisModel(
        range,
        minViewExtent,
        maxViewExtent,
        minimumMajorTickIncrement,
        minimumMajorTickSpacing,
        minorTickCount,
    )
}

/**
 * Calculates an [ClosedFloatingPointRange] that can be used with a [DoubleLinearAxisModel] based on the
 * min/max values of the provided list of Doubles. If the list is empty, returns a range of 0..1.
 */
public fun List<Double>.autoScaleRange(): ClosedFloatingPointRange<Double> {
    if (isEmpty()) {
        return 0.0..1.0
    }
    val max = this.max()
    val min = this.min()
    val range = if (max - min == 0.0) {
        if (min != 0.0) {
            (max * 2.0) - (min / 2.0)
        } else {
            1.0
        }
    } else {
        max - min
    }

    val scale = 10.0.pow(floor(log10(range)))

    val scaleMin = if (min < 0) {
        ceil(abs(min / scale))
    } else {
        floor(abs(min / scale))
    } * scale * sign(min)

    val scaleMax = if (max > 0) {
        ceil(abs(max / scale))
    } else {
        floor(abs(max / scale))
    } * scale * sign(max)

    return if (scaleMax - scaleMin == 0.0) {
        if (scaleMin != 0.0) {
            scaleMin / 2.0..scaleMax * 2.0
        } else {
            scaleMin..1.0
        }
    } else {
        scaleMin..scaleMax
    }
}

/**
 * Calculates a [ClosedFloatingPointRange] that can be used with a [LinearAxisModel] based on the
 * min/max X values of the provided list of [Point]s.
 */
public fun <Y> List<Point<Double, Y>>.autoScaleXRange(): ClosedFloatingPointRange<Double> = toXList().autoScaleRange()

/**
 * Calculates a [ClosedFloatingPointRange] that can be used with a [LinearAxisModel] based on the
 * min/max Y values of the provided list of [Point]s.
 */
public fun <X> List<Point<X, Double>>.autoScaleYRange(): ClosedFloatingPointRange<Double> = toYList().autoScaleRange()
