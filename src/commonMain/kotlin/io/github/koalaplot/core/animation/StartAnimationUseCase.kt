package io.github.koalaplot.core.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * @param executionType Controls the execution of this animation use case.
 * @param chartAnimationSpecs Specifies all animations to use.
 */
public class StartAnimationUseCase(
    private val executionType: ExecutionType = ExecutionType.Default,
    private vararg val chartAnimationSpecs: AnimationSpec<Float>,
) {

    public val animatables: List<Animatable<Float, AnimationVector1D>> by lazy {
        chartAnimationSpecs.map {
            Animatable(getAnimationStartValue())
        }
    }

    @Composable
    public operator fun invoke(key: Any) {
        when (executionType) {
            ExecutionType.Default -> {
                LaunchedEffect(key1 = key) {
                    chartAnimationSpecs.forEachIndexed { index, animationSpec ->
                        animatables[index].animateTo(TARGET_ANIMATION_VALUE, animationSpec)
                    }
                }
            }

            ExecutionType.None -> {
                // no animation start
            }
        }
    }

    private fun getAnimationStartValue(): Float {
        return when (executionType) {
            ExecutionType.Default -> START_ANIMATION_VALUE
            ExecutionType.None -> TARGET_ANIMATION_VALUE
        }
    }

    public enum class ExecutionType {
        Default,
        None,
    }

    private companion object {
        private const val START_ANIMATION_VALUE = 0f
        private const val TARGET_ANIMATION_VALUE = 1f
    }
}
