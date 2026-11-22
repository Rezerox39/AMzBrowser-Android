package com.amz.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amz.browser.ui.theme.LocalAMzPalette
import dev.chrisbanes.haze.HazeState

/**
 * Lightweight glass-styled surface.
 *
 * Backdrop blur was removed: re-blurring a live WebView on every frame was the
 * main cause of jank. The layered gradient + rim + shadow keeps the glass look
 * without the per-frame GPU cost. [hazeState] is kept for call-site compatibility.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    hazeState: HazeState? = null,
    elevation: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalAMzPalette.current
    val shape = RoundedCornerShape(cornerRadius)

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(palette.glassTop, palette.glassBottom)
    )
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
            .background(backgroundBrush)
            .border(width = 1.dp, brush = rimBrush, shape = shape),
        content = content
    )
}
