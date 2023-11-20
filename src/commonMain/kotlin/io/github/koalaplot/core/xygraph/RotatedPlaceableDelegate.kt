package io.github.koalaplot.core.xygraph

import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import io.github.koalaplot.core.util.Vector
import io.github.koalaplot.core.util.rotate
import io.github.koalaplot.core.util.toIntOffset
import kotlin.math.abs

/**
 * A delegate for a Placeable that has rotated content. In Compose, the rotate modifier applies a transformation to
 * the drawing canvas of the composable, but doesn't reflect that in its dimensions or position. This class serves
 * as a delegate to assist in sizing and positioning the content of a Composable that has had rotation applied.
 *
 * The delegate supports 9 anchor points that can be used for positioning and dimensioning. They are located at the
 * top left, top center, top right, right middle, bottom right, bottom center, bottom left, left middle, and center of
 * the original composable, i.e. corresponding to positions relative to the content as if it wasn't rotated. Since
 * rotation occurs about the center, it corresponds to the same point in the content before and after rotation.
 */
internal data class RotatedPlaceableDelegate(val placeable: Placeable, val rotation: Float) {
    // Compute the position of the anchor points relative to the center of the Composable
    private val anchorPositions = buildMap {
        put(AnchorPoint.TopLeft, Vector(-placeable.measuredWidth / 2f, -placeable.measuredHeight / 2f).rotate(rotation))
        put(AnchorPoint.TopCenter, Vector(0f, -placeable.measuredHeight / 2f).rotate(rotation))
        put(AnchorPoint.TopRight, Vector(placeable.measuredWidth / 2f, -placeable.measuredHeight / 2f).rotate(rotation))
        put(AnchorPoint.RightMiddle, Vector(placeable.measuredWidth / 2f, 0f).rotate(rotation))
        put(
            AnchorPoint.BottomRight,
            Vector(placeable.measuredWidth / 2f, placeable.measuredHeight / 2f).rotate(rotation)
        )
        put(AnchorPoint.BottomCenter, Vector(0f, placeable.measuredHeight / 2f).rotate(rotation))
        put(
            AnchorPoint.BottomLeft,
            Vector(-placeable.measuredWidth / 2f, placeable.measuredHeight / 2f).rotate(rotation)
        )
        put(AnchorPoint.LeftMiddle, Vector(-placeable.measuredWidth / 2f, 0f).rotate(rotation))
        put(AnchorPoint.Center, Vector(0f, 0f))
    }

    /**
     * Returns the overall height of the rotated Placeable.
     */
    val height: Int by lazy {
        abs(
            anchorPositions.values.maxOf { it.values[1] } -
                anchorPositions.values.minOf { it.values[1] }
        ).toInt()
    }

    /**
     * Returns the overall width of the rotated Placeable.
     */
    val width: Int by lazy {
        abs(
            anchorPositions.values.maxOf { it.values[0] } -
                anchorPositions.values.minOf { it.values[0] }
        ).toInt()
    }

    /**
     * Returns the height of the component above the specified anchor point
     */
    fun heightAbove(ap: AnchorPoint): Int {
        val top = anchorPositions.values.minOf { it.values[1] } // highest point

        return ((anchorPositions[ap]!!.values[1]) - top).toInt()
    }

    /**
     * Returns the height of the component below the specified anchor point.
     */
    fun heightBelow(ap: AnchorPoint): Int {
        val bottom = anchorPositions.values.maxOf { it.values[1] } // lowest point
        return ((bottom - anchorPositions[ap]!!.values[1])).toInt()
    }

    /**
     * Returns the width of the component to the right of the specified anchor point.
     */
    fun widthRight(ap: AnchorPoint): Int {
        val rightMost = anchorPositions.values.maxOf { it.values[0] }
        return ((rightMost - anchorPositions[ap]!!.values[0])).toInt()
    }

    /**
     * Returns the width of the component to the left of the specified anchor point.
     */
    fun widthLeft(ap: AnchorPoint): Int {
        val leftMost = anchorPositions.values.minOf { it.values[0] }
        return (anchorPositions[ap]!!.values[0] - leftMost).toInt()
    }

    /**
     * Places the rotated Placeable's AnchorPoint [ap] at [x], [y] in its parent's coordinate system.
     */
    fun Placeable.PlacementScope.place(x: Int, y: Int, ap: AnchorPoint, zIndex: Float = 0f) {
        place(IntOffset(x, y), ap, zIndex)
    }

    /**
     * Places the rotated Placeable's AnchorPoint [ap] at [position] in its parent's coordinate system.
     */
    fun Placeable.PlacementScope.place(position: IntOffset, ap: AnchorPoint, zIndex: Float = 0f) {
        // Placeable.place positions the top left corner of the unrotated placeable
        // Compute the offset from the anchor point to that top left corner
        val offset: Vector = (Vector(placeable.width / 2f, placeable.height / 2f) + anchorPositions[ap]!!)
        placeable.place(position - offset.toIntOffset(), zIndex)
    }
}
