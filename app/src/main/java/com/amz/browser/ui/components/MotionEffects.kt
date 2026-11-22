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

/** Stable holder for the live touch glow. Read inside draw blocks only. */
class FingerTracker {
    var position by mutableStateOf(Offset.Unspecified)
}

@Composable
fun AuroraBackground(
    fingerTracker: FingerTracker? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalAMzPalette.current
    val transition = rememberInfiniteTransition(label = "aurora")
    val driftDuration = (16000 * palette.speedFactor).toInt().coerceAtLeast(1000)

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
            animation = tween(durationMillis = driftDuration * 3, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraRotation"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = phase * 2f - 1f
        val breathe = 0.94f + phase * 0.12f
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
                colors = listOf(Color(0xFF5E5CE6).copy(alpha = 0.30f), Color.Transparent),
                center = orbit(0.18f, 0.14f, 0.10f),
                radius = w * 0.56f * breathe
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.accent.copy(alpha = 0.28f), Color.Transparent),
                center = orbit(0.84f, 0.36f, 0.12f),
                radius = w * 0.58f / breathe
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF2D55).copy(alpha = 0.20f), Color.Transparent),
                center = orbit(0.28f, 0.82f, 0.11f),
                radius = w * 0.50f * breathe
            )
        )
        val finger = fingerTracker?.position ?: Offset.Unspecified
        if (finger != Offset.Unspecified) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF64D2FF).copy(alpha = 0.22f), Color.Transparent),
                    center = finger,
                    radius = w * 0.38f * breathe
                )
            )
        }
    }
}
