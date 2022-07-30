package io.github.koalaplot.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.koalaplot.core.legend.LegendLocation
import io.github.koalaplot.core.theme.KoalaPlotTheme
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi

/**
 * A ChartLayout is a UI element consisting of a title, legend, and chart content. The legend
 * may be positioned at different locations within the Chart.
 * @param modifier Modifier to be applied to the layout of the Chart
 * @param title Optional Title to be placed at the top center of the Chart
 * @param legend Optional Legend to be placed on the Chart
 * @param legendLocation Indicates the location of the [legend] within the Chart
 * @param content The Primary Chart content.
 */
@ExperimentalKoalaPlotApi
@Composable
public fun ChartLayout(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    legend: @Composable () -> Unit = {},
    legendLocation: LegendLocation = KoalaPlotTheme.legendLocation,
    content: @Composable () -> Unit = {}
) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            title.invoke()
        }

        if (legendLocation == LegendLocation.TOP) {
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                legend.invoke()
            }
        }

        if (legendLocation == LegendLocation.TOP || legendLocation == LegendLocation.BOTTOM ||
            legendLocation == LegendLocation.NONE
        ) {
            Box(modifier = Modifier.align(Alignment.CenterHorizontally).weight(1.0f, false)) {
                content()
            }
        }

        if (legendLocation == LegendLocation.LEFT || legendLocation == LegendLocation.RIGHT) {
            Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                if (legendLocation == LegendLocation.LEFT) {
                    legend.invoke()
                }
                Box(modifier = Modifier.weight(1.0f)) {
                    content()
                }
                if (legendLocation == LegendLocation.RIGHT) {
                    legend.invoke()
                }
            }
        }

        if (legendLocation == LegendLocation.BOTTOM) {
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                legend.invoke()
            }
        }
    }
}
