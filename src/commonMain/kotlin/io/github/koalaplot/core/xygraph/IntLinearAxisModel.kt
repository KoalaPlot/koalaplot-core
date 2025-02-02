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
import kotlin.math.roundToInt

/**
 * An [AxisModel] that uses Int values and is linear.
 *
 * @param range  The minimum to maximum values allowed to be represented on this Axis. Zoom and
 * scroll modifications may not exceed this range.
 * @param minViewExtent Specifies the minimum allowed viewable range after zooming. Must
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
public class IntLinearAxisModel(
    public override val range: IntRange,
    private val minViewExtent: Int = ((range.last - range.first) * ZoomRangeLimitDefault).coerceAtLeast(1.0).toInt(),
    private val maxViewExtent: Int = ((range.last - range.first)),
    private val minimumMajorTickIncrement: Int = (
        (range.last - range.first) * MinimumMajorTickIncrementDefault
        ).toInt(),
    override val minimumMajorTickSpacing: Dp = 50.dp,
    private val minorTickCount: Int = 4,
    private val allowZooming: Boolean = true,
    private val allowPanning: Boolean = true,
    private val inverted: Boolean = false,
) : DiscreteLinearAxisModel<Int> {
    init {
        require(range.last > range.first) {
            "Axis range end (${range.last}) must be greater than start (${range.first})"
        }
        require(minimumMajorTickSpacing > 0.dp) { "Minimum major tick spacing must be greater than 0 dp" }
        require(minViewExtent > 0) {
            "minViewExtent must be greater than 0"
        }
        require(maxViewExtent > 0 && maxViewExtent >= minViewExtent) {
            "maxViewExtent must be greater than 0 and greater than or equal to minViewExtent"
        }
        require(minViewExtent <= range.last - range.first) { "minViewExtent must be less than or equal to range" }
        require(maxViewExtent <= range.last - range.first) { "maxViewExtent must be less than or equal to range" }
        require(minimumMajorTickIncrement <= range.last - range.first) {
            "minimumMajorTickIncrement must be less than or equal to the axis range"
        }
    }

    // Function used to compute point offsets. Will be modified from the default if inverted is true.
    private var offsetComputer = { point: Int ->
        (
            (point - currentRange.value.start) /
                (currentRange.value.endInclusive - currentRange.value.start)
            ).toFloat()
    }

    init {
        if (inverted) {
            offsetComputer = { point: Int ->
                (
                    (currentRange.value.endInclusive - point) /
                        (currentRange.value.endInclusive - currentRange.value.start)
                    ).toFloat()
            }
        }
    }

    // Internal for testing
    internal var currentRange = mutableStateOf(
        range.first.toDouble()..(range.first + maxViewExtent).toDouble()
    )
    public override val viewRange: State<ClosedRange<Double>> = currentRange

    override fun computeOffset(point: Int): Float = offsetComputer(point)

    /**
     * Computes major tick values based on a minimum tick spacing that is a
     * fraction of the overall axis length.
     *
     * @param minTickSpacing minimum tick spacing, must be greater than 0 and less than or equal to 1.
     */
    private fun computeMajorTickValues(minTickSpacing: Float): List<Int> {
        val tickSpacing = computeMajorTickSpacing(minTickSpacing)

        return buildList {
            if (tickSpacing > 0) {
                var tickCount = floor(currentRange.value.start / tickSpacing).toInt()
                do {
                    val lastTick = tickCount * tickSpacing
                    if (lastTick >= currentRange.value.start && lastTick <= currentRange.value.endInclusive) {
                        add(lastTick)
                    }
                    tickCount++
                } while (lastTick < currentRange.value.endInclusive)
            }
        }
    }

    override fun computeTickValues(axisLength: Dp): TickValues<Int> {
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
        return object : TickValues<Int> {
            override val majorTickValues = if (inverted) majorTickValues.reversed() else majorTickValues
            override val minorTickValues = if (inverted) minorTickValues.reversed() else minorTickValues
        }
    }

    override fun computeValue(offset: Float): Int {
        require(offset in 0f..1f) {
            "Offset ($offset) must be in [0, 1]"
        }
        val start = currentRange.value.start
        val end = currentRange.value.endInclusive

        // Calculation differs based on whether inverted is true or false.
        // Non-inverted:
        //   offset = (point - start) / (end - start)
        //   -> point = start + offset * (end - start)
        // Inverted:
        //   offset = (end - point) / (end - start)
        //   -> point = end - offset * (end - start)
        val pointDouble = if (!inverted) {
            start + offset * (end - start)
        } else {
            end - offset * (end - start)
        }

        // Double → Int Conversion
        // Typically uses roundToInt() and clamps within the axis range.
        val pointInt = pointDouble.roundToInt()
        return pointInt.coerceIn(range.first, range.last)
    }

    private fun computeMajorTickSpacing(minTickSpacing: Float): Int {
        require(minTickSpacing > 0 && minTickSpacing <= 1) {
            "Minimum tick spacing must be greater than 0 and less than or equal to 1"
        }
        val length = currentRange.value.endInclusive - currentRange.value.start
        val magnitude = 10f.pow(floor(log10(length.toFloat())))
        val scaledTickRatios = TickRatios.map { (it * magnitude).roundToInt() }

        // Test scaledTickRatios and pick the first that produces a distance greater than minTickSpacing
        // and an increment greater than minimumMajorTickIncrement
        val tickSpacing = scaledTickRatios.find {
            (it.toFloat() / length.toFloat()) >= minTickSpacing && it >= minimumMajorTickIncrement
        } ?: minimumMajorTickIncrement

        return tickSpacing
    }

    private fun computeMinorTickValues(majorTickValues: List<Int>, majorTickSpacing: Int): List<Int> = buildList {
        if (minorTickCount > 0 && majorTickValues.isNotEmpty()) {
            val minorIncrement = majorTickSpacing / (minorTickCount + 1)

            if (minorIncrement == 0) return@buildList

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
                if (nextTick >= currentRange.value.start && nextTick <= currentRange.value.endInclusive) {
                    add(nextTick)
                }
                i++
            } while (nextTick >= currentRange.value.start && nextTick <= currentRange.value.endInclusive)

            // create ticks before first major tick. if still space in the range
            i = 1
            do {
                val nextTick = majorTickValues.first() - minorIncrement * i
                if (nextTick >= currentRange.value.start && nextTick <= currentRange.value.endInclusive) {
                    add(nextTick)
                }
                i++
            } while (nextTick >= currentRange.value.start && nextTick <= currentRange.value.endInclusive)
        }
    }

    override fun zoom(zoomFactor: Float, pivot: Float) {
        if (!allowZooming || zoomFactor == 1f) return

        require(zoomFactor > 0) { "Zoom amount must be greater than 0" }
        require(pivot in 0.0..1.0) { "Zoom pivot must be between 0 and 1: $pivot" }

        if (zoomFactor > 1f && currentRange.value.endInclusive - currentRange.value.start <= minViewExtent) {
            // Can't zoom in more
        } else if (zoomFactor < 1f && currentRange.value.endInclusive - currentRange.value.start >= maxViewExtent) {
            // Can't zoom out more
        } else {
            // convert pivot to axis range space
            val pivotAxisScale =
                currentRange.value.start + ((currentRange.value.endInclusive - currentRange.value.start) * pivot)

            val newLow = (pivotAxisScale - (pivotAxisScale - currentRange.value.start) / zoomFactor)
            val newHi = (pivotAxisScale + (currentRange.value.endInclusive - pivotAxisScale) / zoomFactor)

            setViewRange(newLow..newHi)
        }
    }

    override fun pan(amount: Float) {
        if (!allowPanning) return

        // convert pan amount to axis range space
        val panAxisScale = ((currentRange.value.endInclusive - currentRange.value.start) * amount)

        // Limit pan amount to not exceed bounds of range
        val panLimitEnd = min(panAxisScale, range.last - currentRange.value.endInclusive)
        val panLimited = max(panLimitEnd, range.first - currentRange.value.start)

        val newLow = (currentRange.value.start + panLimited)
        val newHi = (currentRange.value.endInclusive + panLimited)

        currentRange.value = newLow..newHi
    }

    override fun setViewRange(newRange: ClosedRange<Double>) {
        val newHi = newRange.endInclusive.coerceIn(range.first.toDouble(), range.last.toDouble())
        val newLow = newRange.start.coerceIn(range.first.toDouble(), range.last.toDouble())

        if (newHi - newLow < minViewExtent) {
            val delta = (minViewExtent - (newHi - newLow)) / 2
            currentRange.value = (newLow - delta)..(newHi + delta)
            if (currentRange.value.start < range.first) {
                currentRange.value = range.first.toDouble()..(range.first + minViewExtent).toDouble()
            } else if (currentRange.value.endInclusive > range.last) {
                currentRange.value = (range.last - minViewExtent).toDouble()..range.last.toDouble()
            }
        } else if (newHi - newLow > maxViewExtent) {
            val delta = (newHi - newLow - maxViewExtent) / 2
            currentRange.value = (newLow + delta)..(newHi - delta)
            if (currentRange.value.start < range.first) {
                currentRange.value = range.first.toDouble()..(range.first + maxViewExtent).toDouble()
            } else if (currentRange.value.endInclusive > range.last) {
                currentRange.value = (range.last - maxViewExtent).toDouble()..range.last.toDouble()
            }
        } else {
            currentRange.value = newLow..newHi
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IntLinearAxisModel

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
 * Create and remember an IntLinearAxisModel. See [IntLinearAxisModel] for parameter descriptions.
 */
@Composable
public fun rememberIntLinearAxisModel(
    range: IntRange,
    minViewExtent: Int = ((range.last - range.first) * ZoomRangeLimitDefault).coerceAtLeast(1.0).toInt(),
    maxViewExtent: Int = ((range.last - range.first)),
    minimumMajorTickIncrement: Int = ((range.last - range.first) * MinimumMajorTickIncrementDefault).toInt(),
    minimumMajorTickSpacing: Dp = 50.dp,
    minorTickCount: Int = 4,
    allowZooming: Boolean = true,
    allowPanning: Boolean = true,
): IntLinearAxisModel = remember(
    range,
    minViewExtent,
    minimumMajorTickIncrement,
    minimumMajorTickSpacing,
    minorTickCount,
    allowZooming,
    allowPanning
) {
    IntLinearAxisModel(
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
 * Calculates an [IntRange] that can be used with a [IntLinearAxisModel] based on the
 * min/max values of the provided list of Ints. If the list is empty, returns a range of 0..1.
 */
public fun List<Int>.autoScaleRange(): IntRange {
    if (isEmpty()) {
        return 0..1
    }
    val max = this.max()
    val min = this.min()
    val range = if (max - min == 0) {
        if (min != 0) {
            (max * 2) - (min / 2)
        } else {
            1
        }
    } else {
        max - min
    }
    val scale = 10f.pow(floor(log10(range.toFloat())))

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

    return if (scaleMax - scaleMin == 0.0f) {
        if (scaleMin != 0.0f) {
            floor(scaleMin / 2.0f).toInt()..ceil(scaleMax * 2.0f).toInt()
        } else {
            0..1
        }
    } else {
        floor(scaleMin).toInt()..ceil(scaleMax).toInt()
    }
}

/**
 * Calculates a [ClosedFloatingPointRange] that can be used with a [LinearAxisModel] based on the
 * min/max X values of the provided list of [Point]s.
 */
public fun <Y> List<Point<Int, Y>>.autoScaleXRange(): IntRange = toXList().autoScaleRange()

/**
 * Calculates a [ClosedFloatingPointRange] that can be used with a [LinearAxisModel] based on the
 * min/max Y values of the provided list of [Point]s.
 */
public fun <X> List<Point<X, Int>>.autoScaleYRange(): IntRange = toYList().autoScaleRange()
