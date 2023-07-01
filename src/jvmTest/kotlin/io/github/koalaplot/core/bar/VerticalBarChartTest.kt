package io.github.koalaplot.core.bar

import androidx.compose.ui.test.junit4.createComposeRule
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xychart.XYChart
import io.github.koalaplot.core.xychart.rememberLinearAxisModel
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
            XYChart(
                rememberLinearAxisModel(0f..10f),
                rememberLinearAxisModel(0f..10f),
            ) {
                VerticalBarChart(listOf(listOf(DefaultBarChartEntry(5f, 0f, 10f))))
            }
        }
    }
}
