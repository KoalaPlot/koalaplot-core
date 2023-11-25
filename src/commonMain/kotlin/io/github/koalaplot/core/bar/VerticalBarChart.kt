package io.github.koalaplot.core.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.util.HoverableElementAreaScope
import io.github.koalaplot.core.util.fixedHeight
import io.github.koalaplot.core.util.generateHueColorPalette
import io.github.koalaplot.core.xychart.XYChartScope
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Deprecated("Use DefaultBarPlotEntry", ReplaceWith("DefaultBarPlotEntry"))
public data class DefaultBarChartEntry<X, Y>(
    public override val xValue: X,
    public override val yMin: Y,
    public override val yMax: Y,
) : BarChartEntry<X, Y>

@Deprecated("Use BarPlotEntry", ReplaceWith("BarPlotEntry"))
public interface BarChartEntry<X, Y> {
    /**
     * X-axis value at which the bar should be plotted
     */
    public val xValue: X

    /**
     * The lowest value at which the bar begins on the y-axis
     */
    public val yMin: Y

    /**
     * The highest value at which the bar ends on the y-axis
     */
    public val yMax: Y
}

public interface BarScope : HoverableElementAreaScope

internal class BarScopeImpl(val hoverableElementAreaScope: HoverableElementAreaScope) :
    BarScope, HoverableElementAreaScope by hoverableElementAreaScope

/**
 * Defines a Composable function used to emit a vertical bar.
 * The parameter series is the chart data series index.
 * The parameter index is the element index within the series.
 * The parameter value is the value of the element.
 */
public typealias VerticalBarComposable<E> = @Composable BarScope.(series: Int, index: Int, value: E) -> Unit

/**
 * A VerticalBarChart to be used in an XYChart and that can plot multiple series either side-by-side
 * or stacked as a stacked bar chart.
 *
 * @param X The type of the x-axis values
 * @param Y The type of the y-axis values
 * @param E The type of the data element holding the values for each bar
 * @param series Bar elements where the outer list contains each series of multiple items.
 * The BarChartEntry's xValues should be equal for entries at the same index within each series.
 * @param bar Composable function to emit a bar for each data element, see [VerticalBarComposable].
 * @param stacked If false, bars from the same index position in each series will be laid out
 * side-by-side occupying the space available between adjacent x-axis values. If true, bars from
 * the same index position in each series will be laid out centered on the x-axis value. The yMin
 * and yMax values for each entry are used to define the vertical positioning of each bar, therefore
 * this function can be used to render multiple stacked bar styles depending on their values.
 * @param maxBarGroupWidth The fraction of space between adjacent x-axis bars or bar groups that
 * may be used. Must be between 0 and 1, defaults to 0.9.
 */
@Deprecated("Replace with either GroupedVerticalBarPlot, VerticalBarPlot, or VerticalBarStackPlot")
@Composable
public fun <X, Y, E : BarChartEntry<X, Y>> XYChartScope<X, Y>.VerticalBarChart(
    series: List<List<E>>,
    modifier: Modifier = Modifier,
    bar: VerticalBarComposable<E> = { i, _, _ ->
        val colors = remember(series.size) { generateHueColorPalette(series.size) }
        DefaultVerticalBar(
            brush = SolidColor(colors[i]),
            modifier = Modifier.fillMaxWidth(KoalaPlotTheme.sizes.barWidth)
        )
    },
    stacked: Boolean = false,
    maxBarGroupWidth: Float = 0.9f,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    require(maxBarGroupWidth in 0f..1f) { "maxBarGroupWidth must be between 0 and 1" }

    val barScope = remember { BarScopeImpl(this) }

    // Animation scale factor
    val beta = remember(series) { Animatable(0f) }
    LaunchedEffect(series) {
        beta.animateTo(1f, animationSpec = animationSpec)
    }

    Layout(modifier = modifier, content = {
        series.forEachIndexed { series, elements ->
            elements.forEachIndexed { element, value ->
                Box { with(barScope) { bar(series, element, value) } }
            }
        }
    }) { measurables: List<Measurable>, constraints: Constraints ->
        val barOffsets = HashMap<Placeable, ClosedRange<Int>>()
        var measurableIndex = 0

        // compute width of area for bars at each x-axis value based on major tick spacing
        val barGroupWidth = xAxisState.let {
            if (it.majorTickOffsets.size > 1) {
                it.majorTickOffsets[1] - it.majorTickOffsets[0]
            } else {
                1f
            } * constraints.maxWidth * maxBarGroupWidth
        }

        val seriesPlaceables = series.map { innerSeries ->
            innerSeries.map { element ->
                val barMin = (
                    yAxisModel.computeOffset(element.yMin).coerceIn(0f, 1f) * constraints.maxHeight
                    ).roundToInt()
                val barMax = (
                    yAxisModel.computeOffset(element.yMax).coerceIn(0f, 1f) * constraints.maxHeight
                    ).roundToInt()

                val barWidth = if (!stacked) {
                    barGroupWidth / series.size
                } else {
                    barGroupWidth
                }

                val height = abs(barMax - barMin) * beta.value
                val p = measurables[measurableIndex++].measure(
                    Constraints(minWidth = 0, maxWidth = barWidth.toInt())
                        .fixedHeight(height.roundToInt())
                )
                barOffsets[p] = barMin..barMax
                p
            }
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            if (seriesPlaceables.isEmpty() || seriesPlaceables[0].isEmpty()) return@layout

            seriesPlaceables[0].indices.forEach { elementIndex ->
                val elementsWidth = seriesPlaceables.sumOf { it[elementIndex].width }
                var xOffset = if (!stacked) {
                    xAxisModel.computeOffset(series[0][elementIndex].xValue) *
                        constraints.maxWidth - elementsWidth / 2f
                } else {
                    xAxisState.majorTickOffsets[elementIndex] * constraints.maxWidth
                }

                seriesPlaceables.forEach { placeables ->
                    val xpos = xOffset + if (stacked) {
                        -placeables[elementIndex].width / 2f
                    } else {
                        0f
                    }
                    placeables[elementIndex].place(
                        (xpos).roundToInt(),
                        constraints.maxHeight -
                            (
                                max(
                                    barOffsets[placeables[elementIndex]]?.start ?: 0,
                                    barOffsets[placeables[elementIndex]]?.endInclusive ?: 0
                                ) * beta.value
                                ).roundToInt()
                    )

                    if (!stacked) xOffset += placeables[elementIndex].width
                }
            }
        }
    }
}

/**
 * A default implementation of a bar for bar charts.
 * @param brush The brush to paint the bar with
 * @param shape An optional shape for the bar.
 * @param border An optional border for the bar.
 * @param hoverElement An optional Composable to be displayed over the bar when hovered over by the
 * mouse or pointer.
 */
@Composable
public fun BarScope.DefaultVerticalBar(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
    hoverElement: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier.fillMaxSize()
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .background(brush = brush, shape = shape)
            .clip(shape)
            .hoverableElement(hoverElement)
    )
}

/**
 * A simplified DefaultVerticalBar that uses a Solid Color [color] and default [RectangleShape].
 */
@Composable
public fun BarScope.DefaultVerticalBar(
    color: Color,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
) {
    DefaultVerticalBar(SolidColor(color), shape = shape, border = border)
}

/**
 * Factory function to create a Composable that emits a solid colored bar.
 */
public fun solidBar(
    color: Color,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
): @Composable BarScope.() -> Unit = {
    DefaultVerticalBar(SolidColor(color), shape = shape, border = border)
}
