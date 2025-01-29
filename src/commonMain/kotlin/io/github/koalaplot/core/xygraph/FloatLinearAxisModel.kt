package io.github.koalaplot.core.xygraph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmName
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign

/**
 * An [AxisModel] that uses Float values and is linear.
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
 * @param inverted If the axis coordinates should be inverted so smaller values are at the top/right.
 */
public class FloatLinearAxisModel(
    public override val range: ClosedFloatingPointRange<Float>,
    private val minViewExtent: Float = (range.endInclusive - range.start) * ZoomRangeLimitDefault.toFloat(),
    private val maxViewExtent: Float = ((range.endInclusive - range.start)),
    private val minimumMajorTickIncrement: Float =
        (range.endInclusive - range.start) * MinimumMajorTickIncrementDefault,
    override val minimumMajorTickSpacing: Dp = 50.dp,
    private val minorTickCount: Int = 4,
    private val inverted: Boolean = false,
) : ContinuousLinearAxisModel<Float> {
    init {
        require(range.endInclusive > range.start) {
            "Axis range end (${range.endInclusive}) must be greater than start (${range.start})"
        }
        require(minimumMajorTickSpacing > 0.dp) { "Minimum major tick spacing must be greater than 0 dp" }
        require(minViewExtent > 0f) {
            "minViewExtent must be greater than 0"
        }
        require(maxViewExtent > 0f && maxViewExtent >= minViewExtent) {
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
    private var offsetComputer = { point: Float ->
        (point - currentRange.value.start) / (currentRange.value.endInclusive - currentRange.value.start)
    }

    init {
        if (inverted) {
            offsetComputer = { point: Float ->
                (currentRange.value.endInclusive - point) / (currentRange.value.endInclusive - currentRange.value.start)
            }
        }
    }

    internal var currentRange = mutableStateOf(range.start..(range.start + maxViewExtent))
    public override val viewRange: State<ClosedRange<Float>> = currentRange

    override fun computeOffset(point: Float): Float = offsetComputer(point)

    /**
     * Computes major tick values based on a minimum tick spacing that is a
     * fraction of the overall axis length.
     *
     * @param minTickSpacing minimum tick spacing, must be greater than 0 and less than or equal to 1.
     */
    private fun computeMajorTickValues(minTickSpacing: Float): List<Float> {
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

    override fun computeTickValues(axisLength: Dp): TickValues<Float> {
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
        return object : TickValues<Float> {
            override val majorTickValues = if (inverted) majorTickValues.reversed() else majorTickValues
            override val minorTickValues = if (inverted) minorTickValues.reversed() else minorTickValues
        }
    }

    private fun computeMajorTickSpacing(minTickSpacing: Float): Float {
        require(minTickSpacing > 0 && minTickSpacing <= 1) {
            "Minimum tick spacing must be greater than 0 and less than or equal to 1"
        }
        val length = currentRange.value.endInclusive - currentRange.value.start
        val magnitude = 10f.pow(floor(log10(length)))
        val scaledTickRatios = TickRatios.map { it * magnitude }

        // Test scaledTickRatios and pick the first that produces a distance greater than minTickSpacing
        // and an increment greater than minimumMajorTickIncrement
        val tickSpacing = scaledTickRatios.find {
            it / length >= minTickSpacing && it >= minimumMajorTickIncrement
        } ?: minimumMajorTickIncrement

        return tickSpacing
    }

    private fun computeMinorTickValues(
        majorTickValues: List<Float>,
        majorTickSpacing: Float
    ): List<Float> = buildList {
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

    override fun zoom(zoomFactor: Float, pivot: Float) {
        if (zoomFactor == 1f) return

        require(zoomFactor > 0) { "Zoom amount must be greater than 0" }
        require(pivot in 0.0..1.0) { "Zoom pivot must be between 0 and 1: $pivot" }

        if (zoomFactor > 1f && currentRange.value.endInclusive - currentRange.value.start == minViewExtent) {
            // Can't zoom in more
        } else if (zoomFactor < 1f && currentRange.value.endInclusive - currentRange.value.start == maxViewExtent) {
            // Can't zoom out more
        } else {
            // convert pivot to axis range space
            val pivotAxisScale = (currentRange.value.start) +
                (currentRange.value.endInclusive - currentRange.value.start) * pivot

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

    override fun setViewRange(newRange: ClosedRange<Float>) {
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

        other as FloatLinearAxisModel

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

/**
 * Create and remember a FloatLinearAxisModel. See [FloatLinearAxisModel] for parameter descriptions.
 */
@Composable
public fun rememberFloatLinearAxisModel(
    range: ClosedFloatingPointRange<Float>,
    minViewExtent: Float = (range.endInclusive - range.start) * ZoomRangeLimitDefault.toFloat(),
    maxViewExtent: Float = ((range.endInclusive - range.start)),
    minimumMajorTickIncrement: Float = (range.endInclusive - range.start) * MinimumMajorTickIncrementDefault,
    minimumMajorTickSpacing: Dp = 50.dp,
    minorTickCount: Int = 4,
): FloatLinearAxisModel = remember(
    range,
    minViewExtent,
    minimumMajorTickIncrement,
    minimumMajorTickSpacing,
    minorTickCount,
) {
    FloatLinearAxisModel(
        range,
        minViewExtent,
        maxViewExtent,
        minimumMajorTickIncrement,
        minimumMajorTickSpacing,
        minorTickCount,
    )
}

/**
 * Calculates an [ClosedFloatingPointRange] that can be used with a [FloatLinearAxisModel] based on the
 * min/max values of the provided list of Floats. If the list is empty, returns a range of 0..1.
 */
@JvmName("autoScaleFloatRange")
public fun List<Float>.autoScaleRange(): ClosedFloatingPointRange<Float> {
    if (isEmpty()) {
        return 0f..1f
    }
    val max = this.max()
    val min = this.min()
    val range = if (max - min == 0f) {
        if (min != 0f) {
            (max * 2f) - (min / 2f)
        } else {
            1f
        }
    } else {
        max - min
    }

    val scale = 10f.pow(floor(log10(range)))

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

    return if (scaleMax - scaleMin == 0f) {
        if (scaleMin != 0f) {
            scaleMin / 2f..scaleMax * 2f
        } else {
            scaleMin..1f
        }
    } else {
        scaleMin..scaleMax
    }
}

/**
 * Calculates a [ClosedFloatingPointRange] that can be used with a [LinearAxisModel] based on the
 * min/max X values of the provided list of [Point]s.
 */
public fun <Y> List<Point<Float, Y>>.autoScaleXRange(): ClosedFloatingPointRange<Float> = toXList().autoScaleRange()

/**
 * Calculates a [ClosedFloatingPointRange] that can be used with a [LinearAxisModel] based on the
 * min/max Y values of the provided list of [Point]s.
 */
public fun <X> List<Point<X, Float>>.autoScaleYRange(): ClosedFloatingPointRange<Float> = toYList().autoScaleRange()
