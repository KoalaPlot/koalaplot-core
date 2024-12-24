package io.github.koalaplot.core.animation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * @param chartAnimationSpec Specifies the animation to use when the chart is first drawn.
 * @param labelAnimationSpec Specifies the animation to use when the labels are drawn. Drawing of the labels begins
 * after the chart animation is complete.
 */
public class StartAnimationUseCase(
    private val executionType: ExecutionType = ExecutionType.Default,
    private val chartAnimationSpec: AnimationSpec<Float>,
    private val labelAnimationSpec: AnimationSpec<Float>,
) {

    public val chartAnimationStartValue: Float
        get() = when (executionType) {
            ExecutionType.Default -> START_ANIMATION_VALUE
            ExecutionType.None -> TARGET_ANIMATION_VALUE
        }

    public val labelAnimationStartValue: Float
        get() = when (executionType) {
            ExecutionType.Default -> START_ANIMATION_VALUE
            ExecutionType.None -> TARGET_ANIMATION_VALUE
        }

    @Composable
    public operator fun invoke(
        key: Any,
        onStartAnimation: suspend (AnimationSpecs) -> Unit,
    ) {
        when (executionType) {
            ExecutionType.Default -> {
                executeDefault(
                    key = key,
                    onStartAnimation = onStartAnimation,
                )
            }

            ExecutionType.None -> {
                // no animation start
            }
        }
    }

    @Composable
    private fun executeDefault(
        key: Any,
        onStartAnimation: suspend (AnimationSpecs) -> Unit,
    ) {
        LaunchedEffect(key1 = key) {
            onStartAnimation.invoke(
                AnimationSpecs(
                    chartAnimationSpec = chartAnimationSpec,
                    labelAnimationSpec = labelAnimationSpec,
                )
            )
        }
    }

    public data class AnimationSpecs(
        val chartAnimationSpec: AnimationSpec<Float>,
        val labelAnimationSpec: AnimationSpec<Float>,
    )

    public enum class ExecutionType {
        Default,
        None,
    }

    public companion object {
        public const val START_ANIMATION_VALUE: Float = 0f
        public const val TARGET_ANIMATION_VALUE: Float = 1f
    }
}
