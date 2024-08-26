package io.github.koalaplot.core.pie

import io.github.koalaplot.core.util.Degrees
import org.junit.Assert.assertEquals
import org.junit.Test

private const val NE_MID_ANGLE = -45.0
private const val SE_MID_ANGLE = 45.0
private const val SW_MID_ANGLE = 135.0
private const val NW_MID_ANGLE = 225.0

class CircularLabelPositionProviderTest {
    //region Quadrant utility
    @Test
    fun `Quadrant#from returns the correct Quadrant for angles within standard boundaries`() {
        assertEquals(Quadrant.NorthEast, Quadrant.from(Degrees(NE_MID_ANGLE)))
        assertEquals(Quadrant.SouthEast, Quadrant.from(Degrees(SE_MID_ANGLE)))
        assertEquals(Quadrant.SouthWest, Quadrant.from(Degrees(SW_MID_ANGLE)))
        assertEquals(Quadrant.NorthWest, Quadrant.from(Degrees(NW_MID_ANGLE)))
    }

    /**
     * This test in particular is important, as Quadrants are defined with inclusive ranges, leading to an overlap at
     * the boundaries. Normalization of angles therefore can lead to slightly unexpected results.
     */
    @Test
    fun `Quadrant#from returns the correct Quadrant for angles at the Quadrant boundaries`() {
        // -90 will normalize to 270
        assertEquals(Quadrant.NorthWest, Quadrant.from(Degrees(-90.0)))

        // returns NE as it is the first quadrant covering 0 deg
        assertEquals(Quadrant.NorthEast, Quadrant.from(Degrees(0.0)))

        assertEquals(Quadrant.SouthEast, Quadrant.from(Degrees(0.001)))
        // returns SE as it is the first quadrant covering 90 deg
        assertEquals(Quadrant.SouthEast, Quadrant.from(Degrees(90.0)))

        assertEquals(Quadrant.SouthWest, Quadrant.from(Degrees(90.001)))
        // returns SW as it is the first quadrant covering 180 deg
        assertEquals(Quadrant.SouthWest, Quadrant.from(Degrees(180.0)))

        assertEquals(Quadrant.NorthWest, Quadrant.from(Degrees(180.001)))
        assertEquals(Quadrant.NorthWest, Quadrant.from(Degrees(270.0)))
    }

    @Test
    fun `Quadrant#from normalizes and returns the correct Quadrant for angles greater than 270 degrees`() {
        assertEquals(Quadrant.NorthEast, Quadrant.from(Degrees(275.0)))

        assertEquals(Quadrant.NorthEast, Quadrant.from(Degrees(1 * 360f + NE_MID_ANGLE)))
        assertEquals(Quadrant.SouthEast, Quadrant.from(Degrees(2 * 360f + SE_MID_ANGLE)))
        assertEquals(Quadrant.SouthWest, Quadrant.from(Degrees(3 * 360f + SW_MID_ANGLE)))
        assertEquals(Quadrant.NorthWest, Quadrant.from(Degrees(4 * 360f + NW_MID_ANGLE)))
    }

    @Test
    fun `Quadrant#from normalizes and returns the correct Quadrant for angles less than -90 degrees`() {
        assertEquals(Quadrant.NorthWest, Quadrant.from(Degrees(-95.0)))

        assertEquals(Quadrant.NorthEast, Quadrant.from(Degrees(1 * -360f + NE_MID_ANGLE)))
        assertEquals(Quadrant.SouthEast, Quadrant.from(Degrees(2 * -360f + SE_MID_ANGLE)))
        assertEquals(Quadrant.SouthWest, Quadrant.from(Degrees(3 * -360f + SW_MID_ANGLE)))
        assertEquals(Quadrant.NorthWest, Quadrant.from(Degrees(4 * -360f + NW_MID_ANGLE)))
    }
    //endregion
}
