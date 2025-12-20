package io.github.koalaplot.core.xygraph

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class LongLinearAxisModelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rememberLongLinearAxisModelSetsCorrectDefaultMinViewExtent() {
        composeTestRule.setContent {
            val model = rememberLongLinearAxisModel(0..1L)
            assertEquals(1L, model.minViewExtent)
        }
    }
}
