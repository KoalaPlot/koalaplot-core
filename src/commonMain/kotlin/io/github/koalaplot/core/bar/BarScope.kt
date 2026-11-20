package io.github.koalaplot.core.bar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Receiver scope of the composables emitting the bars of a bar plot. Pass it to bar shapes such as
 * [VerticalPlanoConvexShape] to let them use the exact pixel geometry of the plot.
 */
public interface BarScope

internal class BarScopeImpl : BarScope {
    /**
     * Pixel scale of the dependent axis used by the most recent measure pass of the bar plot, or null before the plot
     * has been measured. Bar shapes read it while drawing to derive exactly the same bar edges as the layout.
     */
    var pixelScale: BarPixelScale? by mutableStateOf(null)
}
