package io.github.koalaplot.core.bar

import androidx.compose.ui.test.junit4.createComposeRule
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.IntLinearAxisModel
import org.junit.Rule
import org.junit.Test

class BulletGraphTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * 5 default qualitative ranges allowed.
     */
    @OptIn(ExperimentalKoalaPlotApi::class)
    @Suppress("MagicNumber")
    @Test
    fun fiveDefaultRangesTest() {
        composeTestRule.setContent {
            BulletGraphs {
                bullet(
                    IntLinearAxisModel(0..100),
                ) {
                    ranges(0, 10, 20, 30, 40, 50)
                }
            }
        }
    }

    /**
     * Trying to use more than 5 qualitative ranges should fail with an IllegalArgumentException.
     */
    @OptIn(ExperimentalKoalaPlotApi::class)
    @Suppress("MagicNumber")
    @Test
    fun moreThanFiveDefaultRangesFailsTest() {
        composeTestRule.setContent {
            var caught = false

            BulletGraphs {
                bullet(
                    IntLinearAxisModel(0..100),
                ) {
                    try {
                        ranges(0, 10, 20, 30, 40, 50, 60)
                    } catch (_: IllegalArgumentException) {
                        caught = true
                    }
                }
            }

            assert(caught)
        }
    }

    /**
     *
     */
    @OptIn(ExperimentalKoalaPlotApi::class)
    @Suppress("MagicNumber")
    @Test
    fun moreThanFiveCustomRangesTest() {
        composeTestRule.setContent {
            BulletGraphs {
                bullet(
                    IntLinearAxisModel(0..100),
                ) {
                    ranges(0) {
                        range(10)
                        range(20)
                        range(30)
                        range(40)
                        range(50)
                        range(60)
                    }
                }
            }
        }
    }
}
