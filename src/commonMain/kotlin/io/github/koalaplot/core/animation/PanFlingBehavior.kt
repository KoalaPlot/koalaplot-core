package io.github.koalaplot.core.animation

import androidx.compose.animation.core.FloatDecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import io.github.koalaplot.core.gestures.getSpeed
import kotlin.math.absoluteValue

private const val MinFlingMovementThreshold = 0.5f

/**
 * A class that handles the inertial fling behavior for pan gestures
 *
 * This class is responsible for animating the fling behavior when the user performs a pan gesture
 * with an initial velocity. It uses an animation spec to handle the decay of the velocity over time
 * and calculates the pan offsets accordingly. The pan offsets are passed to a provided callback block
 * to update the position or perform other actions based on the pan values
 *
 * @param animationSpec The animation spec used to define the decay behavior for the fling animation
 */
internal class PanFlingBehavior(
    val animationSpec: FloatDecayAnimationSpec,
) {

    /**
     * Performs a fling operation based on the initial velocity and updates the pan offset over time
     *
     * This method simulates the inertial fling effect by animating the pan gesture's movement,
     * starting from the initial velocity and gradually decelerating until the pan movement becomes negligible.
     * The resulting pan offsets are provided through the [block] callback function, which is called
     * each time the position is updated
     *
     * @param initialVelocity The initial velocity of the pan gesture
     * @param block A callback function that is invoked with the pan offset (`Offset`) at each step of the fling
     * animation
     * The callback receives the calculated pan offset as a parameter
     */
    suspend fun performFling(initialVelocity: Velocity, block: (pan: Offset) -> Unit) {
        var lastValue = 0f
        val initialSpeed = initialVelocity.getSpeed()
        val direction = initialVelocity / initialSpeed
        animateDecay(
            initialValue = 0.0f,
            initialVelocity = initialSpeed,
            animationSpec = animationSpec,
        ) { value, _ ->
            val delta = value - lastValue
            lastValue = value

            // Skip small movements to prevent unnecessary updates
            if (delta.absoluteValue < MinFlingMovementThreshold) return@animateDecay

            val pan = Offset(
                x = direction.x * delta,
                y = direction.y * delta,
            )
            block(pan)
        }
    }
}
