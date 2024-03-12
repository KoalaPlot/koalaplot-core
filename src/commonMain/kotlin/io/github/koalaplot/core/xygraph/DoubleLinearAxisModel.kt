package io.github.koalaplot.core.xygraph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * @param zoomRangeLimit Specifies the minimum allowed range after zooming. Must
 * be greater than 0 and less than the difference between the start and end of [range].
 * @param minimumMajorTickIncrement The minimum value between adjacent major ticks. Must be less than or equal to
 * the extent of the range.
 * @param minimumMajorTickSpacing Specifies the minimum physical spacing for major ticks, in
 * Dp units. Must be greater than 0.
 * @param minorTickCount The number of minor ticks per major tick interval.
 * @param allowZooming If the axis should allow zooming
 * @param allowPanning If the axis should allow panning.
 */
public class DoubleLinearAxisModel(
    public override val range: ClosedFloatingPointRange<Double>,
    private val zoomRangeLimit: Double = (range.endInclusive - range.start) * ZoomRangeLimitDefault,
    private val minimumMajorTickIncrement: Double =
        (range.endInclusive - range.start) * MinimumMajorTickIncrementDefault,
    override val minimumMajorTickSpacing: Dp = 50.dp,
    private val minorTickCount: Int = 4,
    private val allowZooming: Boolean = true,
    private val allowPanning: Boolean = true,
) : ILinearAxisModel<Double> {
    init {
        require(range.endInclusive > range.start) {
            "Axis range end (${range.endInclusive}) must be greater than start (${range.start})"
        }
        require(minimumMajorTickSpacing > 0.dp) { "Minimum major tick spacing must be greater than 0 dp" }
        require(zoomRangeLimit > 0f) {
            "Zoom range limit must be greater than 0"
        }
        require(zoomRangeLimit < range.endInclusive - range.start) { "Zoom range limit must be less than range" }
        require(minimumMajorTickIncrement <= range.endInclusive - range.start) {
            "minimumMajorTickIncrement must be less than or equal to the axis range"
        }
    }

    private var currentRange by mutableStateOf(range)

    override fun computeOffset(point: Double): Float {
        return ((point - currentRange.start) / (currentRange.endInclusive - currentRange.start)).toFloat()
    }

    /**
     * Computes major tick values based on a minimum tick spacing that is a
     * fraction of the overall axis length.
     *
     * @param minTickSpacing minimum tick spacing, must be greater than 0 and less than or equal to 1.
     */
    private fun computeMajorTickValues(minTickSpacing: Float): List<Double> {
        val tickSpacing = computeMajorTickSpacing(minTickSpacing)

        return buildList {
            if (tickSpacing > 0) {
                var tickCount = floor(currentRange.start / tickSpacing)
                do {
                    val lastTick = tickCount * tickSpacing
                    if (lastTick in currentRange) {
                        add(lastTick)
                    }
                    tickCount++
                } while (lastTick < currentRange.endInclusive)
            }
        }
    }

    override fun computeTickValues(axisLength: Dp): TickValues<Double> {
        val minTickSpacing = (minimumMajorTickSpacing / axisLength).coerceIn(0f..1f)
        val majorTickValues = computeMajorTickValues(minTickSpacing)
        val minorTickValues = computeMinorTickValues(
            majorTickValues,
            computeMajorTickSpacing(minTickSpacing)
        )
        return object : TickValues<Double> {
            override val majorTickValues = majorTickValues
            override val minorTickValues = minorTickValues
        }
    }

    private fun computeMajorTickSpacing(minTickSpacing: Float): Double {
        require(minTickSpacing > 0 && minTickSpacing <= 1) {
            "Minimum tick spacing must be greater than 0 and less than or equal to 1"
        }
        val length = currentRange.endInclusive - currentRange.start
        val magnitude = 10.0.pow(floor(log10(length)))
        val scaledTickRatios = TickRatios.map { it * magnitude }

        // Test scaledTickRatios and pick the first that produces a distance greater than minTickSpacing
        // and an increment greater than minimumMajorTickIncrement
        val tickSpacing = scaledTickRatios.find {
            it / length >= minTickSpacing && it >= minimumMajorTickIncrement
        } ?: minimumMajorTickIncrement

        return tickSpacing
    }

    private fun computeMinorTickValues(
        majorTickValues: List<Double>,
        majorTickSpacing: Double
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
                if (nextTick in currentRange) {
                    add(nextTick)
                }
                i++
            } while (nextTick in currentRange)

            // create ticks before first major tick. if still space in the range
            i = 1
            do {
                val nextTick = majorTickValues.first() - minorIncrement * i
                if (nextTick in currentRange) {
                    add(nextTick)
                }
                i++
            } while (nextTick in currentRange)
        }
    }

    override fun zoom(zoomFactor: Float, pivot: Float) {
        if (!allowZooming || zoomFactor == 1f) return

        require(zoomFactor > 0) { "Zoom amount must be greater than 0" }
        require(pivot in 0.0..1.0) { "Zoom pivot must be between 0 and 1: $pivot" }

        // convert pivot to axis range space
        val pivotAxisScale =
            (currentRange.start) + (currentRange.endInclusive - currentRange.start) * pivot

        val newLow = (pivotAxisScale - (pivotAxisScale - currentRange.start) / zoomFactor).coerceIn(range)
        val newHi = (pivotAxisScale + (currentRange.endInclusive - pivotAxisScale) / zoomFactor).coerceIn(range)

        if (newHi - newLow < zoomRangeLimit) {
            val delta = zoomRangeLimit - (newHi - newLow)
            currentRange = (newLow - delta / 2f)..(newHi + delta / 2f)
            if (currentRange.start < range.start) {
                currentRange = range.start..(range.start + zoomRangeLimit)
            } else if (currentRange.endInclusive > range.endInclusive) {
                currentRange = (range.endInclusive - zoomRangeLimit)..range.endInclusive
            }
        } else {
            currentRange = newLow..newHi
        }
    }

    override fun pan(amount: Float) {
        if (!allowPanning) return

        // convert pan amount to axis range space
        val panAxisScale = (currentRange.endInclusive - currentRange.start) * amount

        // Limit pan amount to not exceed bounds of range
        val panLimitEnd = min(panAxisScale, range.endInclusive - currentRange.endInclusive)
        val panLimited = max(panLimitEnd, range.start - currentRange.start)

        val newLow = (currentRange.start + panLimited)
        val newHi = (currentRange.endInclusive + panLimited)

        currentRange = newLow..newHi
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DoubleLinearAxisModel

        if (range != other.range) return false
        if (zoomRangeLimit != other.zoomRangeLimit) return false
        if (minimumMajorTickIncrement != other.minimumMajorTickIncrement) return false
        if (minimumMajorTickSpacing != other.minimumMajorTickSpacing) return false
        if (minorTickCount != other.minorTickCount) return false
        if (allowZooming != other.allowZooming) return false
        return allowPanning == other.allowPanning
    }

    override fun hashCode(): Int {
        var result = range.hashCode()
        result = 31 * result + zoomRangeLimit.hashCode()
        result = 31 * result + minimumMajorTickIncrement.hashCode()
        result = 31 * result + minimumMajorTickSpacing.hashCode()
        result = 31 * result + minorTickCount
        result = 31 * result + allowZooming.hashCode()
        result = 31 * result + allowPanning.hashCode()
        return result
    }
}

/**
 * Create and remember a DoubleLinearAxisModel. See [DoubleLinearAxisModel] for parameter descriptions.
 */
@Composable
public fun rememberDoubleLinearAxisModel(
    range: ClosedFloatingPointRange<Double>,
    zoomRangeLimit: Double = (range.endInclusive - range.start) * ZoomRangeLimitDefault,
    minimumMajorTickIncrement: Double = (range.endInclusive - range.start) * MinimumMajorTickIncrementDefault,
    minimumMajorTickSpacing: Dp = 50.dp,
    minorTickCount: Int = 4,
    allowZooming: Boolean = true,
    allowPanning: Boolean = true,
): DoubleLinearAxisModel = remember(
    range,
    zoomRangeLimit,
    minimumMajorTickIncrement,
    minimumMajorTickSpacing,
    minorTickCount,
    allowZooming,
    allowPanning
) {
    DoubleLinearAxisModel(
        range,
        zoomRangeLimit,
        minimumMajorTickIncrement,
        minimumMajorTickSpacing,
        minorTickCount,
        allowZooming,
        allowPanning
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
