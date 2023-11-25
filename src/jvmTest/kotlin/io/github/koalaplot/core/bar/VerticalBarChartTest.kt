package io.github.koalaplot.core.bar

import androidx.compose.ui.test.junit4.createComposeRule
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberLinearAxisModel
import org.junit.Rule
import org.junit.Test

class VerticalBarChartTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test creating a bar chart with only 1 data value.
     */
    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun oneBarTest() {
        composeTestRule.setContent {
            XYGraph(
                rememberLinearAxisModel(0f..10f),
                rememberLinearAxisModel(0f..10f),
            ) {
                VerticalBarPlot(
                    listOf(
                        DefaultVerticalBarPlotEntry(
                            5f,
                            DefaultVerticalBarPosition(0f, 10f)
                        )
                    ),
                    bar = {}
                )
            }
        }
    }
}
