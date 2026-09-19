package io.github.koalaplot.core.animation

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimationSpecExtensionsTest {
    @Test
    fun testSnapIsInstantaneous() {
        assertTrue(snap<Float>().isInstantaneous())
    }

    @Test
    fun testSnapWithDelayIsNotInstantaneous() {
        assertFalse(snap<Float>(delayMillis = 50).isInstantaneous())
    }

    @Test
    fun testZeroDurationTweenIsInstantaneous() {
        assertTrue(tween<Float>(durationMillis = 0).isInstantaneous())
    }

    @Test
    fun testTweenIsNotInstantaneous() {
        assertFalse(tween<Float>(durationMillis = 300).isInstantaneous())
    }
}
