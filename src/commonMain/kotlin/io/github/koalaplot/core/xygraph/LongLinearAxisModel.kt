package io.github.koalaplot.core.xygraph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.util.sign
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * An [AxisModel] that uses Long values and is linear.
 *
 * @param range  The minimum to maximum values allowed to be represented on this Axis. Zoom and
 * scroll modifications may not exceed this range.
 * @param minViewExtent Specifies the minimum allowed range after zooming. Must
 * be greater than 0 and less than or equal to the difference between the start and end of [range].
 * @param maxViewExtent Specifies the maximum allowed viewable range. Must be greater than 0, greater
 * than or equal to minViewExtent, and less than or equal to the difference between the start and end of [range].
 * @param minimumMajorTickIncrement The minimum value between adjacent major ticks. Must be less than or equal to
 * the extent of the range.
 * @param minimumMajorTickSpacing Specifies the minimum physical spacing for major ticks, in
 * Dp units. Must be greater than 0.
 * @param minorTickCount The number of minor ticks per major tick interval.
 * @param allowZooming If the axis should allow zooming
 * @param allowPanning If the axis should allow panning.
 * @param inverted If the axis coordinates should be inverted so smaller values are at the top/right.
 */
public class LongLinearAxisModel(
    public override val range: LongRange,
    private val minViewExtent: Long = ((range.last - range.first) * ZoomRangeLimitDefault).coerceAtLeast(1.0).toLong(),
    private val maxViewExtent: Long = ((range.last - range.first)),
    private val minimumMajorTickIncrement: Long = (
        (range.last - range.first) * MinimumMajorTickIncrementDefault
        ).toLong(),
    override val minimumMajorTickSpacing: Dp = 50.dp,
    private val minorTickCount: Int = 4,
    private val allowZooming: Boolean = true,
    private val allowPanning: Boolean = true,
    private val inverted: Boolean = false,
) : LinearAxisModel<Long> {
    init {
        require(range.last > range.first) {
            "Axis range end (${range.last}) must be greater than start (${range.first})"
        }
        require(minimumMajorTickSpacing > 0.dp) { "Minimum major tick spacing must be greater than 0 dp" }
        require(minViewExtent > 0L) {
            "minViewExtent must be greater than 0"
        }
        require(maxViewExtent > 0L && maxViewExtent >= minViewExtent) {
            "maxViewExtent must be greater than 0 and greater than or equal to minViewExtent"
        }
        require(minViewExtent <= range.last - range.first) {
            "minViewExtent must be less than or equal to range"
        }
        require(maxViewExtent <= range.last - range.first) {
            "maxViewExtent must be less than or equal to range"
        }
        require(minimumMajorTickIncrement <= range.last - range.first) {
            "minimumMajorTickIncrement must be less than or equal to the axis range"
        }
    }

    // Function used to compute point offsets. Will be modified from the default if inverted is true.
    private var offsetComputer = { point: Long ->
        (
            (point - currentRange.value.first).toDouble() /
                (currentRange.value.last - currentRange.value.first).toDouble()
            ).toFloat()
    }

    init {
        if (inverted) {
            offsetComputer = { point: Long ->
                (
                    (currentRange.value.last - point).toDouble() /
                        (currentRange.value.last - currentRange.value.first).toDouble()
                    ).toFloat()
            }
        }
    }

    private var currentRange = mutableStateOf(range.first..(range.first + maxViewExtent))
    public override val viewRange: State<ClosedRange<Long>> = currentRange

    override fun computeOffset(point: Long): Float = offsetComputer(point)

    /**
     * Computes major tick values based on a minimum tick spacing that is a
     * fraction of the overall axis length.
     *
     * @param minTickSpacing minimum tick spacing, must be greater than 0 and less than or equal to 1.
     */
    private fun computeMajorTickValues(minTickSpacing: Float): List<Long> {
        val tickSpacing = computeMajorTickSpacing(minTickSpacing)

        return buildList {
            if (tickSpacing > 0) {
                var tickCount = currentRange.value.first / tickSpacing
                do {
                    val lastTick = tickCount * tickSpacing
                    if (lastTick in currentRange.value) {
                        add(lastTick)
                    }
                    tickCount++
                } while (lastTick < currentRange.value.last)
            }
        }
    }

    override fun computeTickValues(axisLength: Dp): TickValues<Long> {
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
        return object : TickValues<Long> {
            override val majorTickValues = if (inverted) majorTickValues.reversed() else majorTickValues
            override val minorTickValues = if (inverted) minorTickValues.reversed() else minorTickValues
        }
    }

    private fun computeMajorTickSpacing(minTickSpacing: Float): Long {
        require(minTickSpacing > 0 && minTickSpacing <= 1) {
            "Minimum tick spacing must be greater than 0 and less than or equal to 1"
        }
        val length = currentRange.value.last - currentRange.value.first
        val magnitude = 10.0.pow(floor(log10(length.toDouble())))
        val scaledTickRatios = TickRatios.map { (it * magnitude).roundToLong() }

        // Test scaledTickRatios and pick the first that produces a distance greater than minTickSpacing
        // and an increment greater than minimumMajorTickIncrement
        val tickSpacing = scaledTickRatios.find {
            (it.toFloat() / length.toFloat()) >= minTickSpacing && it >= minimumMajorTickIncrement
        } ?: minimumMajorTickIncrement

        return tickSpacing
    }

    private fun computeMinorTickValues(majorTickValues: List<Long>, majorTickSpacing: Long): List<Long> = buildList {
        if (minorTickCount > 0 && majorTickValues.isNotEmpty()) {
            val minorIncrement = majorTickSpacing / (minorTickCount + 1)

            if (minorIncrement == 0L) return@buildList

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
        if (!allowZooming || zoomFactor == 1f) return

        require(zoomFactor > 0) { "Zoom amount must be greater than 0" }
        require(pivot in 0.0..1.0) { "Zoom pivot must be between 0 and 1: $pivot" }

        if (zoomFactor > 1f && currentRange.value.last - currentRange.value.first == minViewExtent) {
            // Can't zoom in more
        } else if (zoomFactor < 1f && currentRange.value.last - currentRange.value.first == maxViewExtent) {
            // Can't zoom out more
        } else {
            // convert pivot to axis range space
            val pivotAxisScale = currentRange.value.first +
                ((currentRange.value.last - currentRange.value.first) * pivot.toDouble()).roundToLong()

            val newLow =
                (pivotAxisScale - (pivotAxisScale - currentRange.value.first) / zoomFactor.toDouble()).roundToLong()
            val newHi =
                (pivotAxisScale + (currentRange.value.last - pivotAxisScale) / zoomFactor.toDouble()).roundToLong()

            setViewRange(newLow..newHi)
        }
    }

    override fun pan(amount: Float) {
        if (!allowPanning) return

        // convert pan amount to axis range space
        val panAxisScale = ((currentRange.value.last - currentRange.value.first) * amount.toDouble()).roundToLong()

        // Limit pan amount to not exceed bounds of range
        val panLimitEnd = min(panAxisScale, range.last - currentRange.value.last)
        val panLimited = max(panLimitEnd, range.first - currentRange.value.first)

        val newLow = (currentRange.value.first + panLimited)
        val newHi = (currentRange.value.last + panLimited)

        currentRange.value = newLow..newHi
    }

    override fun setViewRange(newRange: ClosedRange<Long>) {
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

        other as LongLinearAxisModel

        if (range != other.range) return false
        if (minViewExtent != other.minViewExtent) return false
        if (minimumMajorTickIncrement != other.minimumMajorTickIncrement) return false
        if (minimumMajorTickSpacing != other.minimumMajorTickSpacing) return false
        if (minorTickCount != other.minorTickCount) return false
        if (allowZooming != other.allowZooming) return false
        return allowPanning == other.allowPanning
    }

    override fun hashCode(): Int {
        var result = range.hashCode()
        result = 31 * result + minViewExtent.hashCode()
        result = 31 * result + minimumMajorTickIncrement.hashCode()
        result = 31 * result + minimumMajorTickSpacing.hashCode()
        result = 31 * result + minorTickCount
        result = 31 * result + allowZooming.hashCode()
        result = 31 * result + allowPanning.hashCode()
        return result
    }
}

/**
 * Create and remember a LongLinearAxisModel. See [LongLinearAxisModel] for parameter descriptions.
 */
@Composable
public fun rememberLongLinearAxisModel(
    range: LongRange,
    minViewExtent: Long = ((range.last - range.first) * ZoomRangeLimitDefault).toLong(),
    maxViewExtent: Long = ((range.last - range.first)),
    minimumMajorTickIncrement: Long = ((range.last - range.first) * MinimumMajorTickIncrementDefault).toLong(),
    minimumMajorTickSpacing: Dp = 50.dp,
    minorTickCount: Int = 4,
    allowZooming: Boolean = true,
    allowPanning: Boolean = true,
): LongLinearAxisModel = remember(
    range,
    minViewExtent,
    minimumMajorTickIncrement,
    minimumMajorTickSpacing,
    minorTickCount,
    allowZooming,
    allowPanning
) {
    LongLinearAxisModel(
        range,
        minViewExtent,
        maxViewExtent,
        minimumMajorTickIncrement,
        minimumMajorTickSpacing,
        minorTickCount,
        allowZooming,
        allowPanning
    )
}

/**
 * Calculates a [LongRange] that can be used with a [LongLinearAxisModel] based on the
 * min/max values of the provided list of Longs. If the list is empty, returns a range of 0..1.
 */
public fun List<Long>.autoScaleRange(): LongRange {
    if (isEmpty()) {
        return 0L..1L
    }
    val max = this.max()
    val min = this.min()
    val range = if (max - min == 0L) {
        if (min != 0L) {
            (max * 2L) - (min / 2L)
        } else {
            1L
        }
    } else {
        max - min
    }
    val scale = 10.0.pow(floor(log10(range.toDouble())))

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
            floor(scaleMin / 2.0).toLong()..ceil(scaleMax * 2.0).toLong()
        } else {
            0L..1L
        }
    } else {
        floor(scaleMin).toLong()..ceil(scaleMax).toLong()
    }
}

/**
 * Calculates a [ClosedFloatingPointRange] that can be used with a [LinearAxisModel] based on the
 * min/max X values of the provided list of [Point]s.
 */
public fun <Y> List<Point<Long, Y>>.autoScaleXRange(): LongRange = toXList().autoScaleRange()

/**
 * Calculates a [ClosedFloatingPointRange] that can be used with a [LinearAxisModel] based on the
 * min/max Y values of the provided list of [Point]s.
 */
public fun <X> List<Point<X, Long>>.autoScaleYRange(): LongRange = toYList().autoScaleRange()
