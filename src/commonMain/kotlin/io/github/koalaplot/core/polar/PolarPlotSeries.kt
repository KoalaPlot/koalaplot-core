package io.github.koalaplot.core.polar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.HoverableElementAreaScope

/**
 * Plots a series on a [PolarGraph].
 *
 * @param data Data points to display.
 * @param lineStyle Styling for line segments that interconnect the data points. Defaults to null for no lines.
 * @param areaStyle Styling for filling the area created by lines that interconnect the data points. Defaults
 * to null for no fill. If lineStyle is null and areaStyle is not null, the area will be filled without interconnecting
 * lines.
 * @param symbols [Composable] providing for the symbol to draw at each data point. Use null, the default, for no
 * symbols.
 */
@ExperimentalKoalaPlotApi
@Composable
public fun <T> PolarGraphScope<T>.PolarPlotSeries(
    data: List<PolarPoint<Float, T>>,
    modifier: Modifier = Modifier,
    lineStyle: LineStyle? = null,
    areaStyle: AreaStyle? = null,
    symbols: (@Composable HoverableElementAreaScope.(PolarPoint<Float, T>) -> Unit)? = null,
    animationSpec: AnimationSpec<Float> = KoalaPlotTheme.animationSpec
) {
    if (data.isEmpty()) return

    // Animation scale factor
    val beta = remember(data) { Animatable(0f) }
    LaunchedEffect(data) { beta.animateTo(1f, animationSpec = animationSpec) }

    Layout(modifier = modifier, content = {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val area = Path().apply {
                val pt0 = polarToCartesian(data[0], size * beta.value)
                moveTo(pt0.x, pt0.y)
                data.forEach {
                    val pt = polarToCartesian(it, size * beta.value)
                    lineTo(pt.x, pt.y)
                }
                close()
            }
            if (areaStyle != null) {
                drawPath(
                    area,
                    areaStyle.brush,
                    areaStyle.alpha,
                    colorFilter = areaStyle.colorFilter,
                    blendMode = areaStyle.blendMode
                )
            }
            if (lineStyle != null) {
                drawPath(
                    area,
                    lineStyle.brush,
                    lineStyle.alpha,
                    style = Stroke(lineStyle.strokeWidth.toPx(), pathEffect = lineStyle.pathEffect),
                    colorFilter = lineStyle.colorFilter,
                    blendMode = lineStyle.blendMode
                )
            }
        }
        Symbols(data, beta.value, symbols)
    }) { measurables, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {
            // PolarPlot positions this content with the pole at the center of the visible area
            measurables.forEach {
                it.measure(constraints).place(0, 0)
            }
        }
    }
}

@Composable
private fun <T> PolarGraphScope<T>.Symbols(
    data: List<PolarPoint<Float, T>>,
    beta: Float,
    symbol: (@Composable HoverableElementAreaScope.(PolarPoint<Float, T>) -> Unit)? = null,
) {
    if (symbol != null) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                data.indices.forEach {
                    Box { symbol.invoke(this@Symbols, data[it]) }
                }
            }
        ) { measurables: List<Measurable>, constraints: Constraints ->
            val size = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())

            layout(constraints.maxWidth, constraints.maxHeight) {
                data.indices.forEach {
                    val p = measurables[it].measure(constraints.copy(minWidth = 0, minHeight = 0))
                    var position = polarToCartesian(data[it], size * beta)
                    position -= Offset(p.width / 2f, p.height / 2f) // position symbol on-center
                    p.place(position.x.toInt(), position.y.toInt())
                }
            }
        }
    }
}
