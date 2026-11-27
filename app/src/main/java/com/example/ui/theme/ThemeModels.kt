package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class AppThemePreset(
    val displayName: String,
    val subtitle: String,
    val isSeasonal: Boolean,
    val supportsLightVariant: Boolean
) {
    OLED_BLACK("OLED Black", "Pure black AMOLED minimal", false, false),
    PURE_WHITE("Pure White", "Crisp daylight minimalist", false, false),
    PINK("Rose Quartz", "Soft pink refined accents", false, true),
    NAVY_BLUE("Navy Midnight", "Deep academic navy blue", false, false),
    WINTER("Winter Frost", "Cold ice & frost highlights", true, true),
    SPRING("Spring Meadow", "Fresh sage & botanical hues", true, true),
    SUMMER("Summer Solar", "Warm amber & sunlit energy", true, true),
    AUTUMN("Autumn Ember", "Copper, espresso & earth tones", true, true),
    SYSTEM_DEFAULT("System Default", "Follows device dark/light", false, false)
}

enum class ThemeVariant {
    DARK,
    LIGHT
}

data class AccentPreset(
    val id: String,
    val label: String,
    val hex: String
)

val AvailableAccents = listOf(
    AccentPreset("default", "Theme Default", ""),
    AccentPreset("champagne", "Champagne", "#E6D5B8"),
    AccentPreset("ice", "Ice Blue", "#38BDF8"),
    AccentPreset("emerald", "Emerald", "#34D399"),
    AccentPreset("rose", "Rose", "#F472B6"),
    AccentPreset("amber", "Amber", "#FBBF24"),
    AccentPreset("violet", "Violet", "#A78BFA"),
    AccentPreset("coral", "Coral", "#FB7185")
)

data class ThemeSettings(
    val preset: AppThemePreset = AppThemePreset.OLED_BLACK,
    val variant: ThemeVariant = ThemeVariant.DARK,
    val customAccentHex: String = "",
    val seasonalAtmosphereEnabled: Boolean = true
)

@Immutable
data class SyllabyColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val seasonalParticle: Color,
    val isDark: Boolean
)
