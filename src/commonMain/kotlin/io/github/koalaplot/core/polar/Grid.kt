package io.github.koalaplot.core.polar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.lineTo
import io.github.koalaplot.core.util.moveTo
import kotlin.math.min

/**
 * Draws the axes and grid lines on a canvas. This Composable fills max size, so the caller should
 * restrict its size to the required diameter.
 */
@Composable
internal fun <T> PolarGraphScope<T>.Grid(
    polarGraphProperties: PolarGraphProperties,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (polarGraphProperties.background != null) {
            val backgroundPath = generateGridBoundaryPath(size, polarGraphProperties.radialGridType)

            drawPath(
                backgroundPath,
                polarGraphProperties.background.brush,
                polarGraphProperties.background.alpha,
                Fill,
                polarGraphProperties.background.colorFilter,
                polarGraphProperties.background.blendMode
            )
        }

        drawRadialGridLines(
            this@Grid,
            polarGraphProperties.radialAxisGridLineStyle,
            polarGraphProperties.radialGridType,
        )

        drawAngularGridLines(
            this@Grid,
            polarGraphProperties.angularAxisGridLineStyle
        )
    }
}

/**
 * Draws radial grid lines/circles
 */
private fun <T> DrawScope.drawRadialGridLines(
    polarGraphScope: PolarGraphScope<T>,
    style: LineStyle?,
    radialGridType: RadialGridType
) {
    val radii = polarGraphScope.radialAxisModel.tickValues.map { polarGraphScope.radialAxisModel.computeOffset(it) }
    if (radii.isEmpty()) return

    if (style != null) {
        if (radialGridType == RadialGridType.CIRCLES) {
            drawCircularRadialGridLines(polarGraphScope.radialAxisModel, style)
        } else {
            drawStraightRadialGridLines(polarGraphScope, style)
        }
    }
}

private fun <T> DrawScope.drawStraightRadialGridLines(
    polarGraphScope: PolarGraphScope<T>,
    style: LineStyle
) {
    val angles = polarGraphScope.angularAxisModel.getTickValues()
    val radii = polarGraphScope.radialAxisModel.tickValues

    radii.forEach { radius ->
        var startAngle = angles.last()
        for (angleIndex in 0..angles.lastIndex) {
            drawLine(
                start = polarGraphScope.polarToCartesian(PolarPoint(radius, startAngle), size),
                end = polarGraphScope.polarToCartesian(PolarPoint(radius, angles[angleIndex]), size),
                brush = style.brush,
                strokeWidth = style.strokeWidth.toPx(),
                pathEffect = style.pathEffect,
                alpha = style.alpha,
                colorFilter = style.colorFilter,
                blendMode = style.blendMode
            )
            startAngle = angles[angleIndex]
        }
    }
}

private fun DrawScope.drawCircularRadialGridLines(
    radialAxisModel: FloatRadialAxisModel,
    style: LineStyle,
) {
    val radii = radialAxisModel.tickValues.map { radialAxisModel.computeOffset(it) }

    radii.forEach { radius ->
        drawCircle(
            brush = style.brush,
            radius = radius * min(size.width, size.height) / 2.0f,
            center = Offset(0f, 0f),
            alpha = style.alpha,
            style = Stroke(width = style.strokeWidth.toPx(), pathEffect = style.pathEffect),
            colorFilter = style.colorFilter,
            blendMode = style.blendMode
        )
    }
}

private fun <T> DrawScope.drawAngularGridLines(
    polarGraphScope: PolarGraphScope<T>,
    style: LineStyle?
) {
    if (style == null) return

    val radius = polarGraphScope.radialAxisModel.tickValues.last()
    val angles = polarGraphScope.angularAxisModel.getTickValues()

    angles.forEach { angle ->
        drawLine(
            start = Offset(0f, 0f),
            end = polarGraphScope.polarToCartesian(PolarPoint(radius, angle), size),
            brush = style.brush,
            strokeWidth = style.strokeWidth.toPx(),
            pathEffect = style.pathEffect,
            alpha = style.alpha,
            colorFilter = style.colorFilter,
            blendMode = style.blendMode
        )
    }
}

/**
 * Create a path that is the boundary of the grid, depending on the radial grid type. This path is used for
 * setting the clip boundary at the edge of the grid, as well as for drawing the background.
 */
internal fun <T> PolarGraphScope<T>.generateGridBoundaryPath(
    size: Size,
    type: RadialGridType
): Path {
    return if (type == RadialGridType.CIRCLES) {
        Path().apply {
            val scale = min(size.width, size.height)
            @Suppress("MagicNumber")
            this.addArc(Rect(-scale / 2f, -scale / 2f, scale / 2f, scale / 2f), 0f, 360f)
        }
    } else {
        val angles = angularAxisModel.getTickValues()
        val radius = radialAxisModel.tickValues.last()

        Path().apply {
            moveTo(polarToCartesian(PolarPoint(radius, angles.first()), size))

            for (angleIndex in 1..angles.lastIndex) {
                lineTo(polarToCartesian(PolarPoint(radius, angles[angleIndex]), size))
            }

            lineTo(polarToCartesian(PolarPoint(radius, angles.first()), size))
        }
    }
}
