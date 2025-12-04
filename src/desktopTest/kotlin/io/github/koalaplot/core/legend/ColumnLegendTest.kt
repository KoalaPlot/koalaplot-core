package io.github.koalaplot.core.legend

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class ColumnLegendTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test creating a ColumnLegend.
     */
    @Test
    fun basicColumnLegendTest() {
        composeTestRule.setContent {
            ColumnLegend2(5)
        }
    }
}
