package io.github.koalaplot.core.style

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Provides styling for areas.
 *
 * @param brush the color/fill to be applied to the area
 * @param alpha opacity to be applied to the brush from 0.0f to 1.0f representing fully transparent to
 * fully opaque respectively
 * @param colorFilter [ColorFilter] to apply to the brush when drawn into the destination
 * @param blendMode the [BlendMode] blending algorithm to apply to the brush
 **/
public data class AreaStyle(
    val brush: Brush,
    val alpha: Float = 1.0f, // @FloatRange(from = 0.0, to = 1.0)
    val colorFilter: ColorFilter? = null,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode,
)
