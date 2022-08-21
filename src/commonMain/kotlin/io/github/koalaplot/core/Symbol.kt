package io.github.koalaplot.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.AbsoluteCutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import io.github.koalaplot.core.theme.KoalaPlotTheme
import io.github.koalaplot.core.util.DEG2RAD
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import kotlin.math.sin

/**
 * Draws a symbol with a specified [shape], fill using [fillBrush], and outline using [outlineBrush]
 * and [outlineStroke].
 *
 * @param fillBrush Brush to use to fill the shape. Null will result in an unfilled symbol.
 * @param outlineBrush Brush to use for the symbol outline. Null will result in no outline. At least
 * one of fillBrush or outlineBrush must be non-null for the symbol to be drawn.
 * @param alpha Opacity to be applied to the symbol, with `0` being completely transparent and
 * `1` being completely opaque. The value must be between `0` and `1`.
 */
@ExperimentalKoalaPlotApi
@Composable
public fun Symbol(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    size: Dp = KoalaPlotTheme.sizes.symbol,
    fillBrush: Brush? = null,
    outlineBrush: Brush? = null,
    outlineStroke: Stroke = Stroke(),
    alpha: Float = 1.0f
) {
    Box(
        modifier = modifier.size(size).drawBehind {
            val outline = shape.createOutline(this.size, layoutDirection, this)
            fillBrush?.let {
                drawOutline(outline, fillBrush, style = Fill, alpha = alpha)
            }
            outlineBrush?.let {
                drawOutline(outline = outline, outlineBrush, style = outlineStroke, alpha = alpha)
            }
        }.clip(shape)
    )
}

/**
 * Draws a [Symbol] where [size] is specified as a fraction of the maximum incoming constraint dimension.
 */
@ExperimentalKoalaPlotApi
@Composable
public fun Symbol(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    sizeFraction: Float,
    fillBrush: Brush? = null,
    outlineBrush: Brush? = null,
    outlineStroke: Stroke = Stroke(),
    alpha: Float = 1.0f
) {
    BoxWithConstraints {
        val size = if (maxHeight > maxWidth) {
            maxHeight
        } else {
            maxWidth
        } * sizeFraction

        Symbol(modifier, shape, size, fillBrush, outlineBrush, outlineStroke, alpha)
    }
}

// Equilateral triangle angle
private const val AngleEquilateral = 60f

/**
 * A shape describing a triangle.
 */
public val TriangleShape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(
            Path().apply {
                val edgeLength = size.minDimension
                val height = edgeLength * sin(AngleEquilateral * DEG2RAD).toFloat()

                translate(
                    Offset(
                        (size.width - edgeLength) / 2f,
                        size.height - (size.height - height) / 2f
                    )
                )

                moveTo(0f, height)
                lineTo(edgeLength, height)
                lineTo(edgeLength / 2f, 0f)
                lineTo(0f, height)
            }
        )

    override fun toString(): String = "TriangleShape"
}

/**
 * A shape describing a diamond.
 */
public val DiamondShape: Shape = AbsoluteCutCornerShape(50)
