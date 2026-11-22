package com.amz.browser.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.amz.browser.ui.theme.LocalAMzPalette
import kotlinx.coroutines.launch

fun Modifier.jellyClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(enabled, onClick) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    scope.launch {
                        scale.animateTo(
                            targetValue = 0.92f,
                            animationSpec = spring(
                                dampingRatio = 0.75f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    val released = tryAwaitRelease()
                    scope.launch {
                        scale.animateTo(
                            targetValue = 1.04f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
                        )
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                    if (released) onClick()
                }
            )
        }
}

/**
 * Static aurora backdrop. An earlier version animated several full-screen radial
 * gradients every frame, which kept the GPU busy and made scrolling janky.
 * Motion now comes from the tilt-reactive cards and tap feedback instead.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier
) {
    val palette = LocalAMzPalette.current

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF5E5CE6).copy(alpha = 0.28f), Color.Transparent),
                center = Offset(w * 0.18f, h * 0.14f),
                radius = w * 0.58f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.accent.copy(alpha = 0.26f), Color.Transparent),
                center = Offset(w * 0.84f, h * 0.36f),
                radius = w * 0.60f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF2D55).copy(alpha = 0.18f), Color.Transparent),
                center = Offset(w * 0.28f, h * 0.82f),
                radius = w * 0.52f
            )
        )
    }
}
