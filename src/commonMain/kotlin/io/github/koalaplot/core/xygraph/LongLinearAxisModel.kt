package io.github.koalaplot.core.xygraph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * @param zoomRangeLimit Specifies the minimum allowed range after zooming. Must
 * be greater than 0 and less than or equal to the difference between the start and end of [range].
 * @param minimumMajorTickIncrement The minimum value between adjacent major ticks. Must be less than or equal to
 * the extent of the range.
 * @param minimumMajorTickSpacing Specifies the minimum physical spacing for major ticks, in
 * Dp units. Must be greater than 0.
 * @param minorTickCount The number of minor ticks per major tick interval.
 * @param allowZooming If the axis should allow zooming
 * @param allowPanning If the axis should allow panning.
 */
public class LongLinearAxisModel(
    public override val range: LongRange,
    private val zoomRangeLimit: Long = range.last - range.first,
    private val minimumMajorTickIncrement: Long = (
        (range.last - range.first) * MinimumMajorTickIncrementDefault
        ).toLong(),
    override val minimumMajorTickSpacing: Dp = 50.dp,
    private val minorTickCount: Int = 4,
    private val allowZooming: Boolean = true,
    private val allowPanning: Boolean = true,
) : ILinearAxisModel<Long> {
    init {
        require(range.last > range.first) {
            "Axis range end (${range.last}) must be greater than start (${range.first})"
        }
        require(minimumMajorTickSpacing > 0.dp) { "Minimum major tick spacing must be greater than 0 dp" }
        require(zoomRangeLimit > 0f) {
            "Zoom range limit must be greater than 0"
        }
        require(zoomRangeLimit <= range.last - range.first) { "Zoom range limit must be less than or equal to range" }
        require(minimumMajorTickIncrement <= range.last - range.first) {
            "minimumMajorTickIncrement must be less than or equal to the axis range"
        }
    }

    private var currentRange by mutableStateOf(range)

    override fun computeOffset(point: Long): Float {
        return ((point - currentRange.first).toDouble() / (currentRange.last - currentRange.first).toDouble()).toFloat()
    }

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
                var tickCount = currentRange.first / tickSpacing
                do {
                    val lastTick = tickCount * tickSpacing
                    if (lastTick in currentRange) {
                        add(lastTick)
                    }
                    tickCount++
                } while (lastTick < currentRange.last)
            }
        }
    }

    override fun computeTickValues(axisLength: Dp): TickValues<Long> {
        val minTickSpacing = (minimumMajorTickSpacing / axisLength).coerceIn(0f..1f)
        val majorTickValues = computeMajorTickValues(minTickSpacing)
        val minorTickValues = computeMinorTickValues(
            majorTickValues,
            computeMajorTickSpacing(minTickSpacing)
        )
        return object : TickValues<Long> {
            override val majorTickValues = majorTickValues
            override val minorTickValues = minorTickValues
        }
    }

    private fun computeMajorTickSpacing(minTickSpacing: Float): Long {
        require(minTickSpacing > 0 && minTickSpacing <= 1) {
            "Minimum tick spacing must be greater than 0 and less than or equal to 1"
        }
        val length = currentRange.last - currentRange.first
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
            currentRange.first + ((currentRange.last - currentRange.first) * pivot.toDouble()).roundToLong()

        val newLow =
            (pivotAxisScale - (pivotAxisScale - currentRange.first) / zoomFactor.toDouble()).roundToLong()
                .coerceIn(range)
        val newHi =
            (pivotAxisScale + (currentRange.last - pivotAxisScale) / zoomFactor.toDouble()).roundToLong()
                .coerceIn(range)

        if (newHi - newLow < zoomRangeLimit) {
            val delta = zoomRangeLimit - (newHi - newLow)
            currentRange = (newLow - delta / 2L)..(newHi + delta / 2L)
            if (currentRange.first < range.first) {
                currentRange = range.first..(range.first + zoomRangeLimit)
            } else if (currentRange.last > range.last) {
                currentRange = (range.last - zoomRangeLimit)..range.last
            }
        } else {
            currentRange = newLow..newHi
        }
    }

    override fun pan(amount: Float) {
        if (!allowPanning) return

        // convert pan amount to axis range space
        val panAxisScale = ((currentRange.last - currentRange.first) * amount.toDouble()).roundToLong()

        // Limit pan amount to not exceed bounds of range
        val panLimitEnd = min(panAxisScale, range.last - currentRange.last)
        val panLimited = max(panLimitEnd, range.first - currentRange.first)

        val newLow = (currentRange.first + panLimited)
        val newHi = (currentRange.last + panLimited)

        currentRange = newLow..newHi
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as LongLinearAxisModel

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
 * Create and remember a LongLinearAxisModel.
 */
@Composable
public fun rememberLongLinearAxisModel(
    range: LongRange,
    zoomRangeLimit: Long = ((range.last - range.first) * ZoomRangeLimitDefault).toLong(),
    minimumMajorTickIncrement: Long = ((range.last - range.first) * MinimumMajorTickIncrementDefault).toLong(),
    minimumMajorTickSpacing: Dp = 50.dp,
    minorTickCount: Int = 4,
    allowZooming: Boolean = true,
    allowPanning: Boolean = true,
): LongLinearAxisModel = remember(
    range,
    zoomRangeLimit,
    minimumMajorTickIncrement,
    minimumMajorTickSpacing,
    minorTickCount,
    allowZooming,
    allowPanning
) {
    LongLinearAxisModel(
        range,
        zoomRangeLimit,
        minimumMajorTickIncrement,
        minimumMajorTickSpacing,
        minorTickCount,
        allowZooming,
        allowPanning
    )
}

public fun List<Long>.autoScaleRange(): LongRange {
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
