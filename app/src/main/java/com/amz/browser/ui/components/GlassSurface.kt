package com.amz.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amz.browser.ui.theme.LocalAMzPalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    hazeState: HazeState? = null,
    elevation: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalAMzPalette.current
    val tiltController = LocalTiltController.current
    val shape = RoundedCornerShape(cornerRadius)

    val blurModifier = if (palette.glassBlurEnabled && hazeState != null) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = palette.card,
                tint = HazeTint(palette.card.copy(alpha = 0.10f)),
                fallbackTint = HazeTint(palette.card.copy(alpha = 0.65f)),
                blurRadius = 10.dp,
                noiseFactor = 0.01f
            )
        )
    } else {
        Modifier
    }

    val backgroundBrush = if (palette.glassBlurEnabled) {
        Brush.linearGradient(colors = listOf(palette.glassTop, palette.glassBottom))
    } else {
        Brush.linearGradient(colors = listOf(palette.card, palette.card))
    }

    val rimBrush = Brush.linearGradient(
        colors = listOf(palette.rimTop, palette.rimBottom)
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (palette.isDark) 0.48f else 0.14f),
                spotColor = Color.Black.copy(alpha = if (palette.isDark) 0.55f else 0.18f)
            )
            .clip(shape)
            .then(blurModifier)
            .background(backgroundBrush)
            .border(width = 1.dp, brush = rimBrush, shape = shape)
            .drawWithContent {
                drawContent()
                val nx = (tiltController.x.value / 8f).coerceIn(-1f, 1f)
                val ny = (tiltController.y.value / 8f).coerceIn(-1f, 1f)
                val sheenCenter = Offset(
                    x = size.width * (0.5f + nx * 0.42f),
                    y = size.height * (0.25f + ny * 0.42f)
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (palette.isDark) 0.14f else 0.28f),
                            Color.Transparent
                        ),
                        center = sheenCenter,
                        radius = size.maxDimension * 0.8f
                    )
                )
            },
        content = content
    )
}
