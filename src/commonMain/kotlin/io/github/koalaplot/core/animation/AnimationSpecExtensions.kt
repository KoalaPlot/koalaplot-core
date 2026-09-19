package io.github.koalaplot.core.animation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VectorizedDurationBasedAnimationSpec

/**
 * Returns true if this [AnimationSpec] completes on its very first frame, with no delay and no
 * duration, such as [androidx.compose.animation.core.snap]. This can be useful to skip the
 * animation entirely, such as for Compose Previews and/or screenshot testing.
 */
internal fun AnimationSpec<Float>.isInstantaneous(): Boolean {
    val vectorized = vectorize(Float.VectorConverter)
    return vectorized is VectorizedDurationBasedAnimationSpec<AnimationVector1D> &&
        vectorized.delayMillis == 0 &&
        vectorized.durationMillis == 0
}
