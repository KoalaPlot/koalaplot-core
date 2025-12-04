package io.github.koalaplot.core.style

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Provides styling for lines.
 *
 * brush - the color or fill to be applied to the line
 * strokeWidth - stroke width to apply to the line
 * pathEffect - optional effect or pattern to apply to the line
 * alpha - opacity to be applied to the brush from 0.0f to 1.0f representing fully transparent to
 * fully opaque respectively
 * colorFilter - ColorFilter to apply to the brush when drawn into the destination
 * blendMode - the blending algorithm to apply to the brush
 */
public data class LineStyle(
    val brush: Brush,
    val strokeWidth: Dp = 0.dp,
    val pathEffect: PathEffect? = null,
    val alpha: Float = 1.0f,
    val colorFilter: ColorFilter? = null,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode,
)
