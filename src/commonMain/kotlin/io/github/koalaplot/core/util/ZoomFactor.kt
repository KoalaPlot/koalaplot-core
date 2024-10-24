package io.github.koalaplot.core.util

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.jvm.JvmInline

@Stable
@Suppress("FunctionNaming") // expected, because it is a function that provides an object
internal fun ZoomFactor(x: Float, y: Float) = ZoomFactor(packFloats(x, y))

@Immutable
@JvmInline
internal value class ZoomFactor(private val packedValue: Long) {

    @Stable
    val x: Float
        get() {
            check(this.packedValue != Unspecified.packedValue) {
                "ZoomFactor is unspecified"
            }
            return unpackFloat1(packedValue)
        }

    @Stable
    val y: Float
        get() {
            check(this.packedValue != Unspecified.packedValue) {
                "ZoomFactor is unspecified"
            }
            return unpackFloat2(packedValue)
        }

    @Stable
    operator fun component1(): Float = x

    @Stable
    operator fun component2(): Float = y

    fun copy(x: Float = this.x, y: Float = this.y) = ZoomFactor(x, y)

    companion object {
        const val NeutralPoint = 1.0f

        @Stable
        val Neutral = ZoomFactor(NeutralPoint, NeutralPoint)

        @Stable
        val Unspecified = ZoomFactor(Float.NaN, Float.NaN)
    }
}
