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
import androidx.compose.runtime.remember
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

@Composable
fun AuroraBackground(
    fingerPosition: Offset,
    modifier: Modifier = Modifier
) {
    val palette = LocalAMzPalette.current
    val transition = rememberInfiniteTransition(label = "aurora")
    val driftDuration = (14000 * palette.speedFactor).toInt().coerceAtLeast(1000)

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = driftDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = driftDuration * 2, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraRotation"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (driftDuration * 0.65f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraBreathe"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = phase * 2f - 1f
        val rad = Math.toRadians(rotation.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()

        fun orbit(baseX: Float, baseY: Float, radius: Float): Offset {
            val x = baseX + cos * radius * t
            val y = baseY + sin * radius * t
            return Offset(x * w, y * h)
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF5E5CE6).copy(alpha = 0.34f), Color.Transparent),
                center = orbit(0.18f, 0.14f, 0.10f),
                radius = w * 0.66f * breathe
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.accent.copy(alpha = 0.32f), Color.Transparent),
                center = orbit(0.84f, 0.36f, 0.12f),
                radius = w * 0.70f / breathe
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF2D55).copy(alpha = 0.24f), Color.Transparent),
                center = orbit(0.28f, 0.82f, 0.11f),
                radius = w * 0.60f * breathe
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF30D158).copy(alpha = 0.16f), Color.Transparent),
                center = orbit(0.72f, 0.74f, 0.14f),
                radius = w * 0.50f / breathe
            )
        )
        if (fingerPosition != Offset.Unspecified) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF64D2FF).copy(alpha = 0.26f), Color.Transparent),
                    center = fingerPosition,
                    radius = w * 0.45f * breathe
                )
            )
        }
    }
}
