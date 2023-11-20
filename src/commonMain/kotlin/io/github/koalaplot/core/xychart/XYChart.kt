package io.github.koalaplot.core.xychart

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.HoverableElementAreaScope
import io.github.koalaplot.core.util.VerticalRotation
import io.github.koalaplot.core.util.rotateVertically
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.XYGraphScope

@Deprecated(message = "XYChart has been deprecated in favor of XYGraph", replaceWith = ReplaceWith("XYGraph"))
@Composable
public fun <X, Y> XYChart(
    xAxisModel: AxisModel<X>,
    yAxisModel: AxisModel<Y>,
    modifier: Modifier = Modifier,
    xAxisStyle: AxisStyle = rememberAxisStyle(),
    xAxisLabels: @Composable (X) -> Unit,
    xAxisTitle: @Composable () -> Unit = {},
    yAxisStyle: AxisStyle = rememberAxisStyle(),
    yAxisLabels: @Composable (Y) -> Unit,
    yAxisTitle: @Composable () -> Unit = {},
    horizontalMajorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.majorGridlineStyle,
    horizontalMinorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.minorGridlineStyle,
    verticalMajorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.majorGridlineStyle,
    verticalMinorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.minorGridlineStyle,
    panZoomEnabled: Boolean = true,
    content: @Composable XYChartScope<X, Y>.() -> Unit
) {
    XYGraph(
        AxisModelAdapter(xAxisModel),
        AxisModelAdapter(yAxisModel),
        modifier,
        axisStyleAdapter(xAxisStyle),
        xAxisLabels,
        xAxisTitle,
        axisStyleAdapter(yAxisStyle),
        yAxisLabels,
        yAxisTitle,
        horizontalMajorGridLineStyle,
        horizontalMinorGridLineStyle,
        verticalMajorGridLineStyle,
        verticalMinorGridLineStyle,
        panZoomEnabled,
    ) {
        XYGraphScopeAdapter(this).content()
    }
}

private class XYGraphScopeAdapter<X, Y>(val scope: XYGraphScope<X, Y>) : XYChartScope<X, Y> {
    override fun Modifier.hoverableElement(element: @Composable () -> Unit): Modifier {
        return with(scope) {
            this@hoverableElement.hoverableElement(element)
        }
    }

    override val xAxisModel: AxisModel<X>
        get() = InverseAxisModelAdapter(scope.xAxisModel)
    override val yAxisModel: AxisModel<Y>
        get() = InverseAxisModelAdapter(scope.yAxisModel)
    override val xAxisState: AxisState
        get() = InverseAxisStateAdapter(scope.xAxisState)
    override val yAxisState: AxisState
        get() = InverseAxisStateAdapter(scope.yAxisState)
}

internal class XYChartScopeAdapter<X, Y>(val scope: XYChartScope<X, Y>) : XYGraphScope<X, Y> {
    override fun Modifier.hoverableElement(element: @Composable () -> Unit): Modifier {
        return with(scope) {
            this@hoverableElement.hoverableElement(element)
        }
    }

    override val xAxisModel: io.github.koalaplot.core.xygraph.AxisModel<X>
        get() = AxisModelAdapter(scope.xAxisModel)
    override val yAxisModel: io.github.koalaplot.core.xygraph.AxisModel<Y>
        get() = AxisModelAdapter(scope.yAxisModel)
    override val xAxisState: io.github.koalaplot.core.xygraph.AxisState
        get() = AxisStateAdapter(scope.xAxisState)
    override val yAxisState: io.github.koalaplot.core.xygraph.AxisState
        get() = AxisStateAdapter(scope.yAxisState)
}

private class InverseAxisStateAdapter(val state: io.github.koalaplot.core.xygraph.AxisState) : AxisState {
    override val majorTickOffsets: List<Float>
        get() = state.majorTickOffsets
    override val minorTickOffsets: List<Float>
        get() = state.minorTickOffsets
}

private class AxisStateAdapter(val state: AxisState) : io.github.koalaplot.core.xygraph.AxisState {
    override val majorTickOffsets: List<Float>
        get() = state.majorTickOffsets
    override val minorTickOffsets: List<Float>
        get() = state.minorTickOffsets
}

private class InverseAxisModelAdapter<T>(val am: io.github.koalaplot.core.xygraph.AxisModel<T>) : AxisModel<T> {
    override val minimumMajorTickSpacing: Dp
        get() = am.minimumMajorTickSpacing

    override fun computeTickValues(axisLength: Dp): TickValues<T> {
        return InverseTickValuesAdapter(am.computeTickValues(axisLength))
    }

    override fun computeOffset(point: T): Float {
        return am.computeOffset(point)
    }
}

private class AxisModelAdapter<T>(val am: AxisModel<T>) : io.github.koalaplot.core.xygraph.AxisModel<T> {
    override val minimumMajorTickSpacing: Dp
        get() = am.minimumMajorTickSpacing

    override fun computeTickValues(axisLength: Dp): io.github.koalaplot.core.xygraph.TickValues<T> {
        return TickValuesAdapter(am.computeTickValues(axisLength))
    }

    override fun computeOffset(point: T): Float {
        return am.computeOffset(point)
    }
}

private class InverseTickValuesAdapter<T>(val tv: io.github.koalaplot.core.xygraph.TickValues<T>) : TickValues<T> {
    override val majorTickValues: List<T>
        get() = tv.majorTickValues
    override val minorTickValues: List<T>
        get() = tv.minorTickValues
}

private class TickValuesAdapter<T>(val tv: TickValues<T>) : io.github.koalaplot.core.xygraph.TickValues<T> {
    override val majorTickValues: List<T>
        get() = tv.majorTickValues
    override val minorTickValues: List<T>
        get() = tv.minorTickValues
}

private fun axisStyleAdapter(axisStyle: AxisStyle) = io.github.koalaplot.core.xygraph.AxisStyle(
    axisStyle.color,
    axisStyle.majorTickSize,
    axisStyle.minorTickSize,
    tickPositionAdapter(axisStyle.tickPosition),
    axisStyle.lineWidth,
    axisStyle.labelRotation
)

private fun tickPositionAdapter(tickPosition: TickPosition): io.github.koalaplot.core.xygraph.TickPosition {
    return when (tickPosition) {
        TickPosition.Outside -> io.github.koalaplot.core.xygraph.TickPosition.Outside
        TickPosition.Inside -> io.github.koalaplot.core.xygraph.TickPosition.Inside
        TickPosition.None -> io.github.koalaplot.core.xygraph.TickPosition.None
    }
}

/**
 * A scope for XY plots providing axis and state context.
 */
@Deprecated(message = "XYChart has been deprecated in favor of XYGraph", replaceWith = ReplaceWith("XYGraphScope"))
public interface XYChartScope<X, Y> : HoverableElementAreaScope {
    public val xAxisModel: AxisModel<X>
    public val yAxisModel: AxisModel<Y>
    public val xAxisState: AxisState
    public val yAxisState: AxisState

    /**
     * Transforms [point] from [AxisModel] space to display coordinates provided a plot area [size].
     */
    public fun scale(
        point: Point<X, Y>,
        size: Size
    ): Offset {
        return Offset(
            xAxisModel.computeOffset(point.x) * size.width,
            size.height - yAxisModel.computeOffset(point.y) * size.height
        )
    }
}

@Deprecated(message = "XYChart has been deprecated in favor of XYGraph", replaceWith = ReplaceWith("XYGraph"))
@Composable
public fun <X, Y> XYChart(
    xAxisModel: AxisModel<X>,
    yAxisModel: AxisModel<Y>,
    modifier: Modifier = Modifier,
    xAxisStyle: AxisStyle = rememberAxisStyle(),
    xAxisLabels: (X) -> String = { it.toString() },
    xAxisTitle: String = "",
    yAxisStyle: AxisStyle = rememberAxisStyle(),
    yAxisLabels: (Y) -> String = { it.toString() },
    yAxisTitle: String = "",
    horizontalMajorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.majorGridlineStyle,
    horizontalMinorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.minorGridlineStyle,
    verticalMajorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.majorGridlineStyle,
    verticalMinorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.minorGridlineStyle,
    panZoomEnabled: Boolean = true,
    content: @Composable XYChartScope<X, Y>.() -> Unit
) {
    XYChart(
        xAxisModel,
        yAxisModel,
        modifier,
        xAxisStyle,
        xAxisLabels = {
            Text(
                xAxisLabels(it),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        },
        xAxisTitle = {
            Text(
                xAxisTitle,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium
            )
        },
        yAxisStyle,
        yAxisLabels = {
            Text(
                yAxisLabels(it),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        },
        yAxisTitle = {
            Text(
                yAxisTitle,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.rotateVertically(VerticalRotation.COUNTER_CLOCKWISE)
                    .padding(bottom = KoalaPlotTheme.sizes.gap),
            )
        },
        horizontalMajorGridLineStyle,
        horizontalMinorGridLineStyle,
        verticalMajorGridLineStyle,
        verticalMinorGridLineStyle,
        panZoomEnabled,
        content
    )
}
