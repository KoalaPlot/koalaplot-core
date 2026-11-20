package io.github.koalaplot.core.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import io.github.koalaplot.core.util.fixedHeight
import io.github.koalaplot.core.util.fixedWidth
import io.github.koalaplot.core.xygraph.AxisModel
import io.github.koalaplot.core.xygraph.XYGraphScope
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Measure policy for laying out vertical bar plots.
 */
internal class BarPlotMeasurePolicyVertical<X, Y, E : BarPlotGroupedPointEntry<X, Y>>(
    private val xyGraphScope: XYGraphScope<X, Y>,
    private val data: List<E>,
    private val maxBarGroupWidth: Float = 0.9f,
    private val beta: Animatable<Float, AnimationVector1D>,
    private val barScope: BarScopeImpl,
) : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val barGroupInfos: MutableList<BarGroupInfo> = mutableListOf()
        val pixelScale = BarPixelScale(constraints.maxHeight, beta.value)
        // Bar shapes read the scale while drawing. Lookahead passes are skipped since they may use other constraints.
        if (!isLookingAhead) barScope.pixelScale = pixelScale

        data.forEachIndexed { index, element ->
            val barInfos = mutableListOf<BarInfo>()

            val scaledBarWidth =
                (
                    xyGraphScope.computeNeighborDistanceV(index, data) *
                        constraints.maxWidth * maxBarGroupWidth / element.d.size.coerceAtLeast(1)
                ).toInt()

            element.d.forEachIndexed { i, verticalBarPosition ->
                val range = pixelScale.pixelRange(xyGraphScope.yAxisModel, verticalBarPosition)

                val p = measurables[index][i].measure(
                    Constraints(minWidth = 0, maxWidth = scaledBarWidth).fixedHeight(range.length),
                )
                barInfos.add(BarInfo(p, range))
            }
            barGroupInfos.add(BarGroupInfo(scaledBarWidth, barInfos))
        }

        return layout(constraints.maxWidth, constraints.maxHeight) {
            barGroupInfos.forEachIndexed { groupIndex, barGroupInfo ->
                // compute center of bar group & allowed width
                val barGroupCenter = xyGraphScope.xAxisModel.computeOffset(data[groupIndex].i) * constraints.maxWidth
                val barGroupWidth = barGroupInfo.thickness * barGroupInfo.bars.size

                // Compute x-axis position for the bar to be centered within its allocated fraction of the
                // overall bar group width
                var xPos = (barGroupCenter - barGroupWidth / 2).toInt()
                barGroupInfo.bars.forEach { barInfo ->
                    if (barInfo.placeable.height > 0) {
                        // Pixel ranges are measured upward from the bottom of the plot area
                        barInfo.placeable.place(xPos, constraints.maxHeight - barInfo.range.endInclusive)
                    }
                    xPos += barGroupInfo.thickness
                }
            }
        }
    }
}

/**
 * Measure policy for laying out horizontal bar plots.
 */
internal class BarPlotMeasurePolicyHorizontal<X, Y, E : BarPlotGroupedPointEntry<Y, X>>(
    private val xyGraphScope: XYGraphScope<X, Y>,
    private val data: List<E>,
    private val maxBarGroupWidth: Float = 0.9f,
    private val beta: Animatable<Float, AnimationVector1D>,
    private val barScope: BarScopeImpl,
) : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val barGroupInfos: MutableList<BarGroupInfo> = mutableListOf()
        val pixelScale = BarPixelScale(constraints.maxWidth, beta.value)
        // Bar shapes read the scale while drawing. Lookahead passes are skipped since they may use other constraints.
        if (!isLookingAhead) barScope.pixelScale = pixelScale

        data.forEachIndexed { index, element ->
            val barInfos = mutableListOf<BarInfo>()

            val scaledBarThickness =
                (
                    xyGraphScope.computeNeighborDistanceH(index, data) *
                        constraints.maxHeight * maxBarGroupWidth / element.d.size.coerceAtLeast(1)
                ).toInt()

            element.d.forEachIndexed { i, barPosition ->
                val range = pixelScale.pixelRange(xyGraphScope.xAxisModel, barPosition)

                val p = measurables[index][i].measure(
                    Constraints(minHeight = 0, maxHeight = scaledBarThickness).fixedWidth(range.length),
                )
                barInfos.add(BarInfo(p, range))
            }
            barGroupInfos.add(BarGroupInfo(scaledBarThickness, barInfos))
        }

        return layout(constraints.maxWidth, constraints.maxHeight) {
            barGroupInfos.forEachIndexed { groupIndex, barGroupInfo ->
                // compute center of bar group & allowed width
                val barGroupCenter = constraints.maxHeight -
                    xyGraphScope.yAxisModel.computeOffset(data[groupIndex].i) * constraints.maxHeight
                val barGroupThickness = barGroupInfo.thickness * barGroupInfo.bars.size

                // Compute y-axis position for the bar to be centered within its allocated fraction of the
                // overall bar group thickness
                var yPos = (barGroupCenter + barGroupThickness / 2).toInt() - barGroupInfo.thickness
                barGroupInfo.bars.forEach { barInfo ->
                    if (barInfo.placeable.width > 0) {
                        // Pixel ranges are measured rightward from the left edge of the plot area
                        barInfo.placeable.place(barInfo.range.start, yPos)
                    }
                    yPos -= barGroupInfo.thickness
                }
            }
        }
    }
}

/**
 * Specifies the orientation of Bars, either [Horizontal] or [Vertical].
 */
private sealed interface BarOrientation {
    /**
     * Indicates that bars should be rendered horizontally.
     */
    object Horizontal : BarOrientation

    /**
     * Indicates that bars should be rendered vertically.
     */
    object Vertical : BarOrientation
}

/**
 * @param placeable The placeable representing a single bar.
 * @param range The edges of the bar along the dependent axis in pixels, measured from the start of the axis (bottom
 * for vertical bars, left for horizontal bars).
 */
private data class BarInfo(
    val placeable: Placeable,
    val range: ClosedRange<Int>,
)

/**
 * Holds the [BarInfo] items for a bar group, e.g. all bars drawn side by side at the same axis coordinate (e.g.,
 * x-axis for vertical bars).
 */
private data class BarGroupInfo(
    val thickness: Int,
    val bars: List<BarInfo>,
)

/**
 * Maps dependent-axis values of bars onto the integer pixel grid of a plot.
 *
 * Every bar edge is computed from exactly one axis value with [edge], and the size of a bar is the distance between
 * its two edges. Rounding the position and the size of a bar independently instead lets the rounding errors of
 * adjacent bars add up, so that stacked bars sharing an axis value can end up one pixel apart (a visible gap) or
 * overlap. Deriving both from the same edges guarantees that bars sharing an axis value also share their pixel edge,
 * for any axis length and at every step of the start animation. Bar shapes use the same edges to cut bars off exactly
 * at the zero line, see [BarScopeImpl.pixelScale].
 *
 * @param axisLength Length of the dependent axis in pixels.
 * @param progress Current value of the start animation, scaling all bars toward the start of the axis.
 */
internal data class BarPixelScale(
    private val axisLength: Int,
    private val progress: Float,
) {
    /**
     * Returns the position of the bar edge located at the relative axis [offset], in pixels measured from the start
     * of the axis (bottom for vertical bars, left for horizontal bars).
     */
    fun edge(offset: Float): Int = (offset.coerceIn(0f, 1f) * axisLength * progress).roundToInt()
}

/**
 * Returns the edges, in pixels, of a bar at [position] along the axis described by [axisModel].
 */
private fun <T> BarPixelScale.pixelRange(
    axisModel: AxisModel<T>,
    position: BarPosition<T>,
): ClosedRange<Int> {
    val startEdge = edge(axisModel.computeOffset(position.start))
    val endEdge = edge(axisModel.computeOffset(position.end))
    return min(startEdge, endEdge)..max(startEdge, endEdge)
}

/**
 * Distance in pixels between the two edges of a bar.
 */
private val ClosedRange<Int>.length: Int
    get() = endInclusive - start

/**
 * Computes the minimum x-axis distance to the data point neighbors to the data point at [index].
 * If [index] is 0 then the distance to the next point is used. If [index] is the last data point, then
 * the distance to the next to last point is used. If [data] has size=1, then 1 is returned. Otherwise, the
 * minimum between the distance to the previous point and the distance to the next point is returned.
 */
private fun <E : BarPlotGroupedPointEntry<X, Y>, X, Y> XYGraphScope<X, Y>.computeNeighborDistanceV(
    index: Int,
    data: List<E>,
): Float = computeNeighborDistance(
    xAxisModel,
    xAxisState.majorTickOffsets.size,
    index,
    data,
)

private fun <E : BarPlotGroupedPointEntry<I, D>, I, D> XYGraphScope<D, I>.computeNeighborDistanceH(
    index: Int,
    data: List<E>,
): Float = computeNeighborDistance(
    yAxisModel,
    yAxisState.majorTickOffsets.size,
    index,
    data,
)

private fun <E : BarPlotGroupedPointEntry<I, D>, I, D> computeNeighborDistance(
    axisModel: AxisModel<I>,
    numTicks: Int,
    index: Int,
    data: List<E>,
): Float = if (index == 0) {
    if (data.size == 1) {
        1f / numTicks.coerceAtLeast(1)
    } else {
        val center = axisModel.computeOffset(data[index].i)
        val right = axisModel.computeOffset(data[index + 1].i)
        abs(center - right)
    }
} else if (index == data.lastIndex) {
    val center = axisModel.computeOffset(data[index].i)
    val left = axisModel.computeOffset(data[index - 1].i)
    abs(center - left)
} else {
    val left = axisModel.computeOffset(data[index - 1].i)
    val center = axisModel.computeOffset(data[index].i)
    val right = axisModel.computeOffset(data[index + 1].i)

    min(abs(center - left), abs(center - right))
}
