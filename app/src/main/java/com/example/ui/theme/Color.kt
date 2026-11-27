package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

fun parseHexColor(hex: String, fallback: Color): Color {
    if (hex.isBlank()) return fallback
    return try {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

fun resolveSyllabyColors(
    settings: ThemeSettings,
    systemDark: Boolean
): SyllabyColors {
    val effectivePreset = when (settings.preset) {
        AppThemePreset.SYSTEM_DEFAULT -> if (systemDark) AppThemePreset.OLED_BLACK else AppThemePreset.PURE_WHITE
        else -> settings.preset
    }

    val isDark = when (effectivePreset) {
        AppThemePreset.OLED_BLACK, AppThemePreset.NAVY_BLUE -> true
        AppThemePreset.PURE_WHITE -> false
        AppThemePreset.SYSTEM_DEFAULT -> systemDark
        else -> settings.variant == ThemeVariant.DARK
    }

    val base = when (effectivePreset) {
        AppThemePreset.OLED_BLACK, AppThemePreset.SYSTEM_DEFAULT -> SyllabyColors(
            background = Color(0xFF000000),
            surface = Color(0xFF0C0C0E),
            surfaceElevated = Color(0xFF151519),
            borderSubtle = Color(0xFF232329),
            textPrimary = Color(0xFFF5F5F7),
            textSecondary = Color(0xFF9898A6),
            accent = Color(0xFFE6D5B8),
            onAccent = Color(0xFF12100C),
            success = Color(0xFF34D399),
            warning = Color(0xFFFBBF24),
            danger = Color(0xFFF87171),
            seasonalParticle = Color(0xFFE6D5B8),
            isDark = true
        )

        AppThemePreset.PURE_WHITE -> SyllabyColors(
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF6F7F9),
            surfaceElevated = Color(0xFFECEEF2),
            borderSubtle = Color(0xFFDDE1E8),
            textPrimary = Color(0xFF111318),
            textSecondary = Color(0xFF5F6575),
            accent = Color(0xFF18181B),
            onAccent = Color(0xFFFFFFFF),
            success = Color(0xFF059669),
            warning = Color(0xFFD97706),
            danger = Color(0xFFDC2626),
            seasonalParticle = Color(0xFF18181B),
            isDark = false
        )

        AppThemePreset.PINK -> if (isDark) {
            SyllabyColors(
                background = Color(0xFF070408),
                surface = Color(0xFF140C16),
                surfaceElevated = Color(0xFF201324),
                borderSubtle = Color(0xFF38203E),
                textPrimary = Color(0xFFFDF2F8),
                textSecondary = Color(0xFFC09CB5),
                accent = Color(0xFFF472B6),
                onAccent = Color(0xFF240717),
                success = Color(0xFF34D399),
                warning = Color(0xFFFBBF24),
                danger = Color(0xFFFB7185),
                seasonalParticle = Color(0xFFF9A8D4),
                isDark = true
            )
        } else {
            SyllabyColors(
                background = Color(0xFFFFF8FB),
                surface = Color(0xFFFCEEF5),
                surfaceElevated = Color(0xFFF9DFEC),
                borderSubtle = Color(0xFFF1C6DA),
                textPrimary = Color(0xFF2B1220),
                textSecondary = Color(0xFF7B4D64),
                accent = Color(0xFFDB2777),
                onAccent = Color(0xFFFFFFFF),
                success = Color(0xFF059669),
                warning = Color(0xFFD97706),
                danger = Color(0xFFE11D48),
                seasonalParticle = Color(0xFFF472B6),
                isDark = false
            )
        }

        AppThemePreset.NAVY_BLUE -> SyllabyColors(
            background = Color(0xFF050A14),
            surface = Color(0xFF0C1526),
            surfaceElevated = Color(0xFF14213A),
            borderSubtle = Color(0xFF213457),
            textPrimary = Color(0xFFF1F5F9),
            textSecondary = Color(0xFF94A3B8),
            accent = Color(0xFF38BDF8),
            onAccent = Color(0xFF041C2C),
            success = Color(0xFF34D399),
            warning = Color(0xFFFBBF24),
            danger = Color(0xFFF87171),
            seasonalParticle = Color(0xFF7DD3FC),
            isDark = true
        )

        AppThemePreset.WINTER -> if (isDark) {
            SyllabyColors(
                background = Color(0xFF03080E),
                surface = Color(0xFF0B141F),
                surfaceElevated = Color(0xFF132234),
                borderSubtle = Color(0xFF223954),
                textPrimary = Color(0xFFF0F9FF),
                textSecondary = Color(0xFF94B4CC),
                accent = Color(0xFF7DD3FC),
                onAccent = Color(0xFF041F30),
                success = Color(0xFF34D399),
                warning = Color(0xFFFBBF24),
                danger = Color(0xFFF87171),
                seasonalParticle = Color(0xFFBAE6FD),
                isDark = true
            )
        } else {
            SyllabyColors(
                background = Color(0xFFF4FAFF),
                surface = Color(0xFFE8F3FC),
                surfaceElevated = Color(0xFFD9EBFA),
                borderSubtle = Color(0xFFBED8EE),
                textPrimary = Color(0xFF0D2235),
                textSecondary = Color(0xFF476A87),
                accent = Color(0xFF0284C7),
                onAccent = Color(0xFFFFFFFF),
                success = Color(0xFF059669),
                warning = Color(0xFFD97706),
                danger = Color(0xFFDC2626),
                seasonalParticle = Color(0xFF38BDF8),
                isDark = false
            )
        }

        AppThemePreset.SPRING -> if (isDark) {
            SyllabyColors(
                background = Color(0xFF040A06),
                surface = Color(0xFF0D1710),
                surfaceElevated = Color(0xFF16261B),
                borderSubtle = Color(0xFF243D2C),
                textPrimary = Color(0xFFF0FDF4),
                textSecondary = Color(0xFF97B8A1),
                accent = Color(0xFF4ADE80),
                onAccent = Color(0xFF062110),
                success = Color(0xFF4ADE80),
                warning = Color(0xFFFBBF24),
                danger = Color(0xFFF87171),
                seasonalParticle = Color(0xFF86EFAC),
                isDark = true
            )
        } else {
            SyllabyColors(
                background = Color(0xFFF5FCF7),
                surface = Color(0xFFEAF7EE),
                surfaceElevated = Color(0xFFD9F0E1),
                borderSubtle = Color(0xFFBBE0C8),
                textPrimary = Color(0xFF102818),
                textSecondary = Color(0xFF486B54),
                accent = Color(0xFF16A34A),
                onAccent = Color(0xFFFFFFFF),
                success = Color(0xFF15803D),
                warning = Color(0xFFD97706),
                danger = Color(0xFFDC2626),
                seasonalParticle = Color(0xFF4ADE80),
                isDark = false
            )
        }

        AppThemePreset.SUMMER -> if (isDark) {
            SyllabyColors(
                background = Color(0xFF0A0803),
                surface = Color(0xFF171208),
                surfaceElevated = Color(0xFF241C0C),
                borderSubtle = Color(0xFF3E3015),
                textPrimary = Color(0xFFFFFBEB),
                textSecondary = Color(0xFFCBB98F),
                accent = Color(0xFFFBBF24),
                onAccent = Color(0xFF261A02),
                success = Color(0xFF34D399),
                warning = Color(0xFFF59E0B),
                danger = Color(0xFFF87171),
                seasonalParticle = Color(0xFFFDE68A),
                isDark = true
            )
        } else {
            SyllabyColors(
                background = Color(0xFFFFFDF5),
                surface = Color(0xFFFEF7E2),
                surfaceElevated = Color(0xFFFCEDC5),
                borderSubtle = Color(0xFFF3DA9B),
                textPrimary = Color(0xFF291D05),
                textSecondary = Color(0xFF785E24),
                accent = Color(0xFFD97706),
                onAccent = Color(0xFFFFFFFF),
                success = Color(0xFF059669),
                warning = Color(0xFFB45309),
                danger = Color(0xFFDC2626),
                seasonalParticle = Color(0xFFFBBF24),
                isDark = false
            )
        }

        AppThemePreset.AUTUMN -> if (isDark) {
            SyllabyColors(
                background = Color(0xFF0A0503),
                surface = Color(0xFF170E09),
                surfaceElevated = Color(0xFF251710),
                borderSubtle = Color(0xFF3F261A),
                textPrimary = Color(0xFFFFF7ED),
                textSecondary = Color(0xFFC6A18E),
                accent = Color(0xFFFB923C),
                onAccent = Color(0xFF291003),
                success = Color(0xFF34D399),
                warning = Color(0xFFFBBF24),
                danger = Color(0xFFF87171),
                seasonalParticle = Color(0xFFFDBA74),
                isDark = true
            )
        } else {
            SyllabyColors(
                background = Color(0xFFFFF9F4),
                surface = Color(0xFFFAEDE2),
                surfaceElevated = Color(0xFFF4DFCD),
                borderSubtle = Color(0xFFE7C4A8),
                textPrimary = Color(0xFF2E170B),
                textSecondary = Color(0xFF7C4D34),
                accent = Color(0xFFEA580C),
                onAccent = Color(0xFFFFFFFF),
                success = Color(0xFF059669),
                warning = Color(0xFFD97706),
                danger = Color(0xFFDC2626),
                seasonalParticle = Color(0xFFFB923C),
                isDark = false
            )
        }
    }

    val finalAccent = parseHexColor(settings.customAccentHex, base.accent)
    val onFinalAccent = if (settings.customAccentHex.isNotBlank()) {
        // Compute perceived luminance so text on custom accent is always readable
        val luminance = (0.299f * finalAccent.red + 0.587f * finalAccent.green + 0.114f * finalAccent.blue)
        if (luminance > 0.58f) Color(0xFF0D0D10) else Color(0xFFFFFFFF)
    } else {
        base.onAccent
    }

    return base.copy(accent = finalAccent, onAccent = onFinalAccent)
}

fun SyllabyColors.toMaterialColorScheme(): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.16f),
            onPrimaryContainer = accent,
            secondary = accent.copy(alpha = 0.85f),
            onSecondary = onAccent,
            secondaryContainer = surfaceElevated,
            onSecondaryContainer = textPrimary,
            tertiary = success,
            onTertiary = Color(0xFF042217),
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceElevated,
            onSurfaceVariant = textSecondary,
            outline = borderSubtle,
            outlineVariant = borderSubtle.copy(alpha = 0.5f),
            error = danger,
            onError = Color(0xFF280606)
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.12f),
            onPrimaryContainer = accent,
            secondary = accent.copy(alpha = 0.85f),
            onSecondary = onAccent,
            secondaryContainer = surfaceElevated,
            onSecondaryContainer = textPrimary,
            tertiary = success,
            onTertiary = Color.White,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceElevated,
            onSurfaceVariant = textSecondary,
            outline = borderSubtle,
            outlineVariant = borderSubtle.copy(alpha = 0.5f),
            error = danger,
            onError = Color.White
        )
    }
}
