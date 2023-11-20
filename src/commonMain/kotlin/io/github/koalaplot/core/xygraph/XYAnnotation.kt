package io.github.koalaplot.core.xygraph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints
import io.github.koalaplot.core.style.LineStyle

/**
 * Places a [Composable] as an annotation on an [XYGraph] with the [Composable]'s
 * [anchorPoint] placed at the provided [location].
 */
@Composable
public fun <X, Y> XYGraphScope<X, Y>.XYAnnotation(
    location: Point<X, Y>,
    anchorPoint: AnchorPoint,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            Box { content() }
        }
    ) { measurables: List<Measurable>, constraints: Constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {
            val size = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
            val p = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))

            val position = scale(location, size) + when (anchorPoint) {
                is AnchorPoint.BottomCenter -> Offset(-p.width / 2f, -p.height.toFloat())
                is AnchorPoint.BottomLeft -> Offset(0f, -p.height.toFloat())
                is AnchorPoint.BottomRight -> Offset(-p.width.toFloat(), -p.height.toFloat())
                is AnchorPoint.Center -> Offset(-p.width / 2f, -p.height / 2f)
                is AnchorPoint.LeftMiddle -> Offset(0f, -p.height / 2f)
                is AnchorPoint.TopCenter -> Offset(-p.width / 2f, 0f)
                is AnchorPoint.TopLeft -> Offset(0f, 0f)
                is AnchorPoint.TopRight -> Offset(-p.width.toFloat(), 0f)
                is AnchorPoint.RightMiddle -> Offset(-p.width.toFloat(), -p.height / 2f)
            }

            p.place(position.x.toInt(), position.y.toInt())
        }
    }
}

/**
 * Places a vertical line marker on the plot at the specified x-axis [location] styled using the provided [lineStyle].
 */
@Composable
public fun <X, Y> XYGraphScope<X, Y>.VerticalLineAnnotation(
    location: X,
    lineStyle: LineStyle
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val x = xAxisModel.computeOffset(location) * size.width

        drawLine(
            brush = lineStyle.brush,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = lineStyle.strokeWidth.toPx(),
            pathEffect = lineStyle.pathEffect,
            alpha = lineStyle.alpha,
            colorFilter = lineStyle.colorFilter,
            blendMode = lineStyle.blendMode
        )
    }
}

/**
 * Places a vertical line marker on the plot at the specified x-axis [location] styled using the provided [lineStyle].
 */
@Composable
public fun <X, Y> XYGraphScope<X, Y>.HorizontalLineAnnotation(
    location: Y,
    lineStyle: LineStyle
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val y = yAxisModel.computeOffset(location) * size.height

        drawLine(
            brush = lineStyle.brush,
            start = Offset(0f, size.height - y),
            end = Offset(size.width, size.height - y),
            strokeWidth = lineStyle.strokeWidth.toPx(),
            pathEffect = lineStyle.pathEffect,
            alpha = lineStyle.alpha,
            colorFilter = lineStyle.colorFilter,
            blendMode = lineStyle.blendMode
        )
    }
}
