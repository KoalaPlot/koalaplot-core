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
) : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val barGroupInfos: MutableList<BarGroupInfo> = mutableListOf()

        data.forEachIndexed { index, element ->
            val barInfos = mutableListOf<BarInfo>()

            val scaledBarWidth =
                (
                    xyGraphScope.computeNeighborDistanceV(index, data) *
                        constraints.maxWidth * maxBarGroupWidth / element.d.size.coerceAtLeast(1)
                ).toInt()

            element.d.forEachIndexed { i, verticalBarPosition ->
                val barMin = (
                    xyGraphScope.yAxisModel
                        .computeOffset(verticalBarPosition.start)
                        .coerceIn(0f, 1f) * constraints.maxHeight
                ).roundToInt()
                val barMax = (
                    xyGraphScope.yAxisModel
                        .computeOffset(verticalBarPosition.end)
                        .coerceIn(0f, 1f) * constraints.maxHeight
                ).roundToInt()

                val height = abs(barMax - barMin) * beta.value

                val p = measurables[index][i].measure(
                    Constraints(minWidth = 0, maxWidth = scaledBarWidth).fixedHeight(height.roundToInt()),
                )
                barInfos.add(BarInfo(p, barMin..barMax))
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
                barGroupInfo.bars.forEachIndexed { index, barInfo ->
                    if (barInfo.placeable.height > 0) {
                        barInfo.placeable.place(
                            xPos,
                            constraints.maxHeight - (
                                max(
                                    barGroupInfo.bars[index].range.start,
                                    barGroupInfo.bars[index].range.endInclusive,
                                ) * beta.value
                            ).roundToInt(),
                        )
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
) : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val barGroupInfos: MutableList<BarGroupInfo> = mutableListOf()

        data.forEachIndexed { index, element ->
            val barInfos = mutableListOf<BarInfo>()

            val scaledBarThickness =
                (
                    xyGraphScope.computeNeighborDistanceH(index, data) *
                        constraints.maxHeight * maxBarGroupWidth / element.d.size.coerceAtLeast(1)
                ).toInt()

            element.d.forEachIndexed { i, barPosition ->
                val barMin = (
                    xyGraphScope.xAxisModel
                        .computeOffset(barPosition.start)
                        .coerceIn(0f, 1f) * constraints.maxWidth
                ).roundToInt()
                val barMax = (
                    xyGraphScope.xAxisModel
                        .computeOffset(barPosition.end)
                        .coerceIn(0f, 1f) * constraints.maxWidth
                ).roundToInt()

                val length = abs(barMax - barMin) * beta.value

                val p = measurables[index][i].measure(
                    Constraints(minHeight = 0, maxHeight = scaledBarThickness).fixedWidth(length.roundToInt()),
                )
                barInfos.add(BarInfo(p, barMin..barMax))
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
                barGroupInfo.bars.forEachIndexed { index, barInfo ->
                    if (barInfo.placeable.width > 0) {
                        barInfo.placeable.place(
                            (
                                min(
                                    barGroupInfo.bars[index].range.start,
                                    barGroupInfo.bars[index].range.endInclusive,
                                ) * beta.value
                            ).roundToInt(),
                            yPos,
                        )
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
 * @param range The range of the bar in pixel coordinates.
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
