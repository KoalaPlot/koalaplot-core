package io.github.koalaplot.core.xychart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.util.times
import kotlin.jvm.JvmName
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign

private val TickRatios = listOf(0.1f, 0.2f, 0.5f, 1f, 2f)

private const val ZoomRangeLimitDefault = 0.2f
private const val MinimumMajorTickIncrementDefault = 0.1f

/**
 * An [AxisModel] that uses Float values and is linear.
 *
 * @param range  The minimum to maximum values allowed to be represented on this Axis. Zoom and
 * scroll modifications may not exceed this range.
 * @param zoomRangeLimit Specifies the minimum allowed range after zooming. Must
 * be greater than 0 and less than the difference between the start and end of [range].
 * @param minimumMajorTickIncrement The minimum value between adjacent major ticks.
 * @param minimumMajorTickSpacing Specifies the minimum physical spacing for major ticks, in
 * Dp units. Must be greater than 0.
 * @param minorTickCount The number of minor ticks per major tick interval.
 * @param allowZooming If the axis should allow zooming
 * @param allowPanning If the axis should allow panning.
 */
public class LinearAxisModel constructor(
    private val range: ClosedFloatingPointRange<Float>,
    private val zoomRangeLimit: Float =
        (range.endInclusive - range.start) * ZoomRangeLimitDefault,
    private val minimumMajorTickIncrement: Float =
        (range.endInclusive - range.start) * MinimumMajorTickIncrementDefault,
    override val minimumMajorTickSpacing: Dp = 50.dp,
    private val minorTickCount: Int = 4,
    private val allowZooming: Boolean = true,
    private val allowPanning: Boolean = true,
) : AxisModel<Float> {
    init {
        require(range.endInclusive > range.start) { "Axis end must be greater than start" }
        require(minimumMajorTickSpacing > 0.dp) { "Minimum major tick spacing must be greater than 0 dp" }
        require(zoomRangeLimit > 0f) {
            "Zoom range limit must be greater than 0"
        }
        require(zoomRangeLimit < range.endInclusive - range.start) { "Zoom range limit must be less than range" }
    }

    private var currentRange by mutableStateOf(range)

    override fun computeOffset(point: Float): Float {
        return (point - currentRange.start) / (currentRange.endInclusive - currentRange.start)
    }

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
                var i = 0
                do {
                    val lastTick =
                        floor((currentRange.start + i * tickSpacing) / tickSpacing) * tickSpacing

                    if (lastTick in currentRange) {
                        add(lastTick)
                    }
                    i++
                } while (lastTick < currentRange.endInclusive)
            }
        }
    }

    override fun computeTickValues(axisLength: Dp): TickValues<Float> {
        val minTickSpacing = (minimumMajorTickSpacing / axisLength).coerceIn(0f..1f)
        val majorTickValues = computeMajorTickValues(minTickSpacing)
        val minorTickValues = computeMinorTickValues(
            majorTickValues, computeMajorTickSpacing(minTickSpacing)
        )
        return TickValues(majorTickValues, minorTickValues)
    }

    private fun computeMajorTickSpacing(minTickSpacing: Float): Float {
        require(minTickSpacing > 0 && minTickSpacing <= 1) {
            "Minimum tick spacing must be greater than 0 and less than or equal to 1"
        }
        val length = currentRange.endInclusive - currentRange.start
        val magnitude = 10f.pow(floor(log10(length)))
        val scaledTickRatios = TickRatios * magnitude

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
            var i = 0
            do {
                val nextTick = majorTickValues.last() + minorIncrement * i
                if (nextTick in currentRange) {
                    add(nextTick)
                }
                i++
            } while (nextTick in currentRange)

            // create ticks before first major tick. if still space in the range
            i = 0
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

        val newLow =
            (
                pivotAxisScale - (pivotAxisScale - currentRange.start) / zoomFactor
                ).coerceIn(range)
        val newHi =
            (
                pivotAxisScale +
                    (currentRange.endInclusive - pivotAxisScale) / zoomFactor
                ).coerceIn(range)

        if (newHi - newLow < zoomRangeLimit) {
            val delta = zoomRangeLimit - (newHi - newLow)
            currentRange =
                (newLow - delta / 2f)..(newHi + delta / 2f)
            if (currentRange.start < range.start) {
                currentRange = range.start..(range.start + zoomRangeLimit)
            } else if (currentRange.endInclusive > range.endInclusive) {
                currentRange =
                    (range.endInclusive - zoomRangeLimit)..range.endInclusive
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
}

/**
 * Create and remember a LinearAxisModel.
 */
@Composable
public fun rememberLinearAxisModel(
    range: ClosedFloatingPointRange<Float>,
    zoomRangeLimit: Float = (range.endInclusive - range.start) * ZoomRangeLimitDefault,
    minimumMajorTickIncrement: Float = (range.endInclusive - range.start) * MinimumMajorTickIncrementDefault,
    minimumMajorTickSpacing: Dp = 50.dp,
    minorTickCount: Int = 4,
    allowZooming: Boolean = true,
    allowPanning: Boolean = true,
): LinearAxisModel = remember {
    LinearAxisModel(
        range, zoomRangeLimit, minimumMajorTickIncrement, minimumMajorTickSpacing,
        minorTickCount, allowZooming, allowPanning
    )
}

@JvmName("autoScaleFloatRange")
public fun List<Float>.autoScaleRange(): ClosedFloatingPointRange<Float> {
    val max = this.max()
    val min = this.min()
    val range = max - min
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

    return scaleMin..scaleMax
}

@JvmName("autoScaleIntRange")
public fun List<Int>.autoScaleRange(): ClosedFloatingPointRange<Float> {
    val max = this.max()
    val min = this.min()
    val range = max - min
    val scale = 10f.pow(floor(log10(range.toFloat())))

    val scaleMin = if (min < 0) {
        ceil(abs(min / scale))
    } else {
        floor(abs(min / scale))
    } * scale * sign(min.toFloat())

    val scaleMax = if (max > 0) {
        ceil(abs(max / scale))
    } else {
        floor(abs(max / scale))
    } * scale * sign(max.toFloat())

    return scaleMin..scaleMax
}
