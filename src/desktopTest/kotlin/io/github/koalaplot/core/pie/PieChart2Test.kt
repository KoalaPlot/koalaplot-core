package io.github.koalaplot.core.pie

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.toDegrees
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("MagicNumber")
class PieChart2Test {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test creating a pie chart with default slice (auto colors).
     */
    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun defaultColorsTest() {
        composeTestRule.setContent {
            PieChart {
                item(value = 1f, label = { })
                item(value = 2f, label = { })
                item(value = 3f, label = { })
            }
        }
    }

    /**
     * Test creating a pie chart with custom slice.
     */
    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun customSliceTest() {
        composeTestRule.setContent {
            PieChart {
                item(value = 1f, slice = { DefaultSlice(androidx.compose.ui.graphics.Color.Red) })
            }
        }
    }

    /**
     * Test creating a pie chart with data of all zeros.
     */
    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun zeroDataTest() {
        composeTestRule.setContent {
            PieChart {
                item(0f)
                item(0f)
            }
        }
    }

    /**
     * Test creating a pie chart with 5 values, last one zero.
     */
    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun lastZeroDataTest() {
        composeTestRule.setContent {
            PieChart {
                item(5f)
                item(6f)
                item(7f)
                item(8f)
                item(0f)
            }
        }
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun removeSliceTest() {
        @Suppress("MagicNumber")
        val pieData = mutableStateListOf(1f, 2f, 3f)

        composeTestRule.setContent {
            PieChart {
                pieData.forEach { value ->
                    item(
                        value = value,
                        key = value, // Use value as key for stability in this test
                        label = { },
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        pieData.removeAt(2)
        composeTestRule.waitForIdle()
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun addSliceTest() {
        @Suppress("MagicNumber")
        val pieData = mutableStateListOf(1f, 2f, 3f)
        composeTestRule.setContent {
            PieChart {
                pieData.forEach { value ->
                    item(
                        value = value,
                        key = value,
                        label = { },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        @Suppress("MagicNumber")
        pieData.add(4f)
        composeTestRule.waitForIdle()
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun animateValueUpdateTest() {
        var capturedAngle1 = 0f
        var capturedPieSize = androidx.compose.ui.unit.IntSize.Zero
        var value1 by mutableFloatStateOf(50f)
        val value2 = 50f

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            PieChart(
                modifier = Modifier.onSizeChanged {
                    capturedPieSize = it
                },
            ) {
                item(
                    value = value1,
                    key = "item1",
                    slice = {
                        capturedAngle1 = pieSliceData.angle
                            .toDegrees()
                            .value
                            .toFloat()
                    },
                )
                item(
                    value = value2,
                    key = "item2",
                )
            }
        }

        // Wait for intro animation to finish
        composeTestRule.mainClock.advanceTimeBy(2000)
        composeTestRule.waitForIdle()

        // At 50/50, angle should be 180
        assertEquals(180f, capturedAngle1, 0.1f)
        val initialSize = capturedPieSize

        // Change value1 to 150. Total = 200. item1 should be 150/200 * 360 = 270.
        value1 = 150f
        composeTestRule.waitForIdle()

        // At t=0 of the update animation, it should still be 180 and size should be initial
        assertEquals(180f, capturedAngle1, 0.1f)
        assertEquals(initialSize, capturedPieSize)

        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.waitForIdle()

        assertTrue(capturedAngle1 > 180f, "Angle should have increased")
        assertTrue(capturedAngle1 < 270f, "Angle should not have reached target yet")

        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.waitForIdle()

        assertEquals(270f, capturedAngle1, 0.1f)
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun animateResizeTest() {
        var capturedPieSize by mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)
        var containerSize by mutableStateOf(200.dp)

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            androidx.compose.foundation.layout.Box(modifier = Modifier.size(containerSize)) {
                PieChart(
                    modifier = Modifier.onSizeChanged {
                        capturedPieSize = it
                    },
                ) {
                    item(value = 50f, key = "item1")
                    item(value = 50f, key = "item2")
                }
            }
        }

        // Wait for intro
        composeTestRule.mainClock.advanceTimeBy(2000)
        composeTestRule.waitForIdle()

        val initialPieSize = capturedPieSize
        assertTrue(initialPieSize.width > 0)

        // Resize container larger
        containerSize = 400.dp
        composeTestRule.waitForIdle()

        // Should animate larger
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.waitForIdle()
        assertTrue(capturedPieSize.width > initialPieSize.width, "Pie should be growing")

        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.waitForIdle()
        val largePieSize = capturedPieSize
        assertTrue(largePieSize.width > initialPieSize.width)

        // Resize container smaller (instant shrink)
        containerSize = 50.dp
        composeTestRule.waitForIdle()

        // Target diameter should eventually be small
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.waitForIdle()
        assertTrue(capturedPieSize.width < largePieSize.width, "Pie should have shrunk")
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun forceCenteredPieAsymmetricTest() {
        var holeCenterInRoot = androidx.compose.ui.geometry.Offset.Zero
        var layoutCenterInRoot = androidx.compose.ui.geometry.Offset.Zero
        val fixedSize = 600.dp

        composeTestRule.setContent {
            androidx.compose.foundation.layout.Box(
                Modifier.size(fixedSize).onGloballyPositioned {
                    layoutCenterInRoot = it.localToRoot(
                        androidx.compose.ui.geometry
                            .Offset(it.size.width / 2f, it.size.height / 2f),
                    )
                },
            ) {
                PieChart(
                    modifier = Modifier.fillMaxSize(),
                    centeredAlignment = true,
                    holeSize = 0.5f,
                    holeContent = {
                        androidx.compose.foundation.layout.Box(
                            Modifier.fillMaxSize().onGloballyPositioned {
                                holeCenterInRoot = it.localToRoot(
                                    androidx.compose.ui.geometry
                                        .Offset(it.size.width / 2f, it.size.height / 2f),
                                )
                            },
                        )
                    },
                ) {
                    item(
                        value = 1f,
                        label = {
                            // Huge label on the right
                            androidx.compose.foundation.layout
                                .Box(Modifier.size(200.dp))
                        },
                    )
                    item(value = 1f) // Small label on the left
                }
            }
        }

        composeTestRule.waitForIdle()

        assertEquals(layoutCenterInRoot.x, holeCenterInRoot.x, 1f, "Pie should be horizontally centered in parent")
    }

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Test
    fun animateValueShrinkTest() {
        var capturedPieSize by mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)
        var value1 by mutableFloatStateOf(10f)
        val value2 = 90f

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            androidx.compose.foundation.layout.Box(modifier = Modifier.size(500.dp)) {
                PieChart(
                    modifier = Modifier.onSizeChanged {
                        capturedPieSize = it
                    },
                    centeredAlignment = false,
                ) {
                    item(value = value1, key = "item1", label = {
                        // Large label that will move as value1 changes
                        androidx.compose.foundation.layout
                            .Box(Modifier.size(200.dp))
                    })
                    item(value = value2, key = "item2")
                }
            }
        }

        // Wait for intro
        composeTestRule.mainClock.advanceTimeBy(2000)
        composeTestRule.waitForIdle()

        val initialSize = capturedPieSize.width
        assertTrue(initialSize > 0)

        // Change value1 to 50. Total = 140.
        // Label moves, potentially shrinking the allowed pie diameter.
        value1 = 50f
        composeTestRule.waitForIdle()

        // At t=100ms, it should have started shrinking but not finished
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.waitForIdle()

        val midSize = capturedPieSize.width
        assertTrue(midSize != initialSize, "Pie size should be changing. initial=$initialSize, mid=$midSize")

        // Wait for completion
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.waitForIdle()

        val finalSize = capturedPieSize.width
        assertTrue(finalSize != midSize, "Pie size should have finished transition. mid=$midSize, final=$finalSize")
    }
}
