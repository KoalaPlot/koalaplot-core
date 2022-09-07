package io.github.koalaplot.core.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private class Sender(private val onMessageReceived: (@Composable () -> Unit, Boolean) -> Unit) {
    fun sendMessage(composable: @Composable () -> Unit, display: Boolean) {
        onMessageReceived(composable, display)
    }
}

//
// private val ModifierLocalSender: ProvidableModifierLocal<Sender> =
//    modifierLocalOf<Sender> { error("No sender provided by parent.") }

/**
 * HoverableElementArea implements a component whose children may optionally provide a hover element
 * to be displayed at the position of the mouse cursor when hovered over child component. A
 * displayed hovered element will be confined to the bounds of the parent HoverableElementArea but
 * may display over multiple children.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
public fun HoverableElementArea(
    modifier: Modifier = Modifier,
    content: @Composable HoverableElementAreaScope.() -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(Offset.Zero) }
    var hoverElement: (@Composable () -> Unit)? by remember { mutableStateOf(null) }

    val scope = remember {
        HoverableElementAreaScopeImpl(
            Sender { composable, display ->
                isHovered = display
                hoverElement = composable
            }
        )
    }

    Layout(
        modifier = modifier.pointerPosition(Unit) { position = it }
        // localProvider exhibiting inconsistent behavior between platforms and package location
        /*.modifierLocalProvider(ModifierLocalSender) {
            Sender { composable, display ->
                isHovered = display
                hoverElement = composable
            }
        }*/,
        content = {
            Box {
                scope.content()
            }
            AnimatedVisibility(isHovered, enter = fadeIn(), exit = fadeOut()) {
                Crossfade(targetState = hoverElement) {
                    it?.invoke()
                }
            }
        }
    ) { measurables, constraints ->
        val contentConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeable = measurables[0].measure(contentConstraints)
        val hoverItem = if (measurables.size == 2) {
            measurables[1].measure(contentConstraints)
        } else {
            null
        }

        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)

            hoverItem?.apply {
                val left = (position.x.toInt() - width / 2)
                    .coerceAtMost(constraints.maxWidth - width).coerceAtLeast(0)

                place(
                    left,
                    (position.y.toInt() - height).coerceAtLeast(0),
                    1f
                )
            }
        }
    }
}

private fun Modifier.pointerPosition(key1: Any?, update: (Offset) -> Unit): Modifier = composed {
    Modifier.pointerInput(key1) {
        val currentContext = currentCoroutineContext()
        awaitPointerEventScope {
            while (currentContext.isActive) {
                val event = awaitPointerEvent()
                update(event.changes.last().position)
            }
        }
    }
}

/**
 * Scope for the HoverableElementArea providing the hoverableElement [Modifier] to enable
 * hovering element functionality and to specify the [Composable] to be displayed on hover.
 */
public interface HoverableElementAreaScope {
    public fun Modifier.hoverableElement(element: @Composable () -> Unit): Modifier
}

private class HoverableElementAreaScopeImpl(private val sender: Sender) :
    HoverableElementAreaScope {
    override fun Modifier.hoverableElement(element: @Composable () -> Unit): Modifier = composed {
        val interactionSource = remember { MutableInteractionSource() }
        var hoverInteraction by remember { mutableStateOf<HoverInteraction.Enter?>(null) }
        // var sender by remember { mutableStateOf<Sender?>(null) }

        fun emitEnter() {
            if (hoverInteraction == null) {
                val interaction = HoverInteraction.Enter()
                sender.sendMessage(element, true)
                hoverInteraction = interaction
            }
        }

        fun emitExit() {
            if (hoverInteraction != null) {
                sender.sendMessage(element, false)
                hoverInteraction = null
            }
        }

        DisposableEffect(interactionSource) {
            onDispose {
                sender.sendMessage(element, false)
            }
        }

        // modifierLocalConsumer was exhibiting inconsistent behavior between platforms and
        // package location of this code
        Modifier/*.modifierLocalConsumer { sender = ModifierLocalSender.current }*/.pointerInput(
            interactionSource
        ) {
            coroutineScope {
                val currentContext = currentCoroutineContext()
                val outerScope = this
                awaitPointerEventScope {
                    while (currentContext.isActive) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> outerScope.launch(
                                start = CoroutineStart.UNDISPATCHED
                            ) {
                                emitEnter()
                            }
                            PointerEventType.Exit -> outerScope.launch(
                                start = CoroutineStart.UNDISPATCHED
                            ) {
                                emitExit()
                            }
                        }
                    }
                }
            }
        }
    }
}
