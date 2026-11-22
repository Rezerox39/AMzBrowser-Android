package com.amz.browser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class ThemeMode { AUTO, LIGHT, DARK }
enum class AnimationSpeed(val factor: Float) { CALM(1.6f), FLUID(1.0f), SNAPPY(0.5f) }

val AMzAccents = listOf(
    0xFF0A84FFL,
    0xFFBF5AF2L,
    0xFFFF2D55L,
    0xFFFF9F0AL,
    0xFF30D158L
)

@Immutable
data class AMzPalette(
    val isDark: Boolean,
    val background: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val fill: Color,
    val divider: Color,
    val accent: Color,
    val success: Color,
    val destructive: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val rimTop: Color,
    val rimBottom: Color,
    val glassBlurEnabled: Boolean,
    val speedFactor: Float
)

val LocalAMzPalette = staticCompositionLocalOf {
    AMzPalette(
        isDark = false,
        background = Color(0xFFF2F2F7),
        card = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF000000),
        textSecondary = Color(0xCC3C3C43),
        fill = Color(0x24787880),
        divider = Color(0x243C3C43),
        accent = Color(0xFF0A84FF),
        success = Color(0xFF30D158),
        destructive = Color(0xFFFF453A),
        glassTop = Color(0xEBFFFFFF),
        glassBottom = Color(0xC7FFFFFF),
        rimTop = Color(0x8CFFFFFF),
        rimBottom = Color(0x1FFFFFFF),
        glassBlurEnabled = true,
        speedFactor = 1.0f
    )
}

private val AMzTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.4).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun AMzTheme(
    themeMode: ThemeMode,
    accentHex: Long,
    glassBlurEnabled: Boolean,
    speedFactor: Float,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = Color(accentHex)

    val palette = if (isDark) {
        AMzPalette(
            isDark = true,
            background = Color(0xFF000000),
            card = Color(0xFF1C1C1E),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xC2EBEBF5),
            fill = Color(0x47787880),
            divider = Color(0x80545458),
            accent = accent,
            success = Color(0xFF30D158),
            destructive = Color(0xFFFF453A),
            glassTop = Color(0xD63A3A40),
            glassBottom = Color(0xC726262A),
            rimTop = Color(0x8CFFFFFF),
            rimBottom = Color(0x1FFFFFFF),
            glassBlurEnabled = glassBlurEnabled,
            speedFactor = speedFactor
        )
    } else {
        AMzPalette(
            isDark = false,
            background = Color(0xFFF2F2F7),
            card = Color(0xFFFFFFFF),
            textPrimary = Color(0xFF000000),
            textSecondary = Color(0xCC3C3C43),
            fill = Color(0x24787880),
            divider = Color(0x243C3C43),
            accent = accent,
            success = Color(0xFF30D158),
            destructive = Color(0xFFFF453A),
            glassTop = Color(0xEBFFFFFF),
            glassBottom = Color(0xC7FFFFFF),
            rimTop = Color(0x8CFFFFFF),
            rimBottom = Color(0x1FFFFFFF),
            glassBlurEnabled = glassBlurEnabled,
            speedFactor = speedFactor
        )
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accent,
            background = palette.background,
            surface = palette.card,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            error = palette.destructive
        )
    } else {
        lightColorScheme(
            primary = accent,
            background = palette.background,
            surface = palette.card,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            error = palette.destructive
        )
    }

    CompositionLocalProvider(LocalAMzPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AMzTypography,
            content = content
        )
    }
}
