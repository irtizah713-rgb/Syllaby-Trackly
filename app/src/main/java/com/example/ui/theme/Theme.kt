package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalSyllabyColors = compositionLocalOf {
    resolveSyllabyColors(ThemeSettings(), systemDark = true)
}

val SyllabyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SyllabyTracklyTheme(
    settings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val syllabyColors = resolveSyllabyColors(settings, systemDark)
    val colorScheme = syllabyColors.toMaterialColorScheme()

    CompositionLocalProvider(LocalSyllabyColors provides syllabyColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = SyllabyShapes
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
                if (settings.preset.isSeasonal && settings.seasonalAtmosphereEnabled) {
                    SeasonalAtmosphereOverlay(
                        preset = settings.preset,
                        tint = syllabyColors.seasonalParticle
                    )
                }
            }
        }
    }
}

@Composable
fun SeasonalAtmosphereOverlay(
    preset: AppThemePreset,
    tint: Color
) {
    // Lightweight, non-interactive subtle atmospheric accents at the top edge
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val dots = listOf(
            Offset(w * 0.12f, h * 0.06f) to 3.5f,
            Offset(w * 0.28f, h * 0.11f) to 2.5f,
            Offset(w * 0.45f, h * 0.04f) to 3.0f,
            Offset(w * 0.67f, h * 0.09f) to 4.0f,
            Offset(w * 0.84f, h * 0.05f) to 2.5f,
            Offset(w * 0.91f, h * 0.14f) to 3.2f,
            Offset(w * 0.19f, h * 0.18f) to 2.0f,
            Offset(w * 0.74f, h * 0.17f) to 2.4f
        )
        when (preset) {
            AppThemePreset.WINTER -> {
                dots.forEach { (pt, r) ->
                    drawCircle(color = tint.copy(alpha = 0.16f), radius = r * 2.2f, center = pt)
                }
            }
            AppThemePreset.SPRING -> {
                dots.forEach { (pt, r) ->
                    drawCircle(color = tint.copy(alpha = 0.14f), radius = r * 2.6f, center = pt)
                }
            }
            AppThemePreset.SUMMER -> {
                drawCircle(
                    color = tint.copy(alpha = 0.06f),
                    radius = w * 0.42f,
                    center = Offset(w * 0.88f, h * 0.04f)
                )
            }
            AppThemePreset.AUTUMN -> {
                dots.forEach { (pt, r) ->
                    drawCircle(color = tint.copy(alpha = 0.13f), radius = r * 2.5f, center = pt)
                }
            }
            else -> Unit
        }
    }
}
