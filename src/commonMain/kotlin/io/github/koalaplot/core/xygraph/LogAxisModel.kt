package io.github.koalaplot.core.xygraph

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.log
import kotlin.math.pow

private const val Base = 10.0
private val MinorTickScale = 2..9

/**
 * A logarithmic axis.
 *
 * @param range  The minimum to maximum values allowed to be represented on this Axis expressed as
 * exponents with a base of 10. For example a range of -1..3 represents an axis range of 0.1 to 1000
 * (10^-1..10^3).
 */
public class LogAxisModel constructor(
    private val range: ClosedRange<Int>,
    override val minimumMajorTickSpacing: Dp = 50.dp,
) : AxisModel<Float> {
    init {
        require(range.endInclusive > range.start) { "Axis end must be greater than start" }
    }

    override fun computeOffset(point: Float): Float {
        return (
            (log(point.toDouble(), Base) - range.start) /
                (range.endInclusive - range.start)
            ).toFloat()
    }

    private fun computeMinorTickValues(majorTickValues: List<Float>): List<Float> = buildList {
        for (tick in 0 until majorTickValues.lastIndex) {
            val init = majorTickValues[tick]
            for (i in MinorTickScale) {
                add(init * i)
            }
        }
    }

    override fun computeTickValues(axisLength: Dp): TickValues<Float> {
        val majorTickValues = buildList {
            for (i in range.start..range.endInclusive) {
                add(Base.pow(i).toFloat())
            }
        }

        return object : TickValues<Float> {
            override val majorTickValues = majorTickValues
            override val minorTickValues = computeMinorTickValues(majorTickValues)
        }
    }
}
