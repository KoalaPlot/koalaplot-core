package io.github.koalaplot.core.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * Remembers an [Animatable] that animates from 0f to 1f using the provided [animationSpec],
 * restarting whenever [key] changes, for use as a chart's reveal progress. If [animationSpec] [is
 * instantaneous][isInstantaneous], such as [androidx.compose.animation.core.snap], this starts
 * already at 1f and skips the animation entirely.
 */
@Composable
internal fun rememberChartRevealAnimation(
    animationSpec: AnimationSpec<Float>,
    key: Any? = null,
): Animatable<Float, AnimationVector1D> {
    val instantaneous = animationSpec.isInstantaneous()
    val animatable = remember(key) {
        Animatable(if (instantaneous) 1f else 0f)
    }
    LaunchedEffect(key) {
        if (!instantaneous) {
            animatable.animateTo(1f, animationSpec = animationSpec)
        }
    }
    return animatable
}
