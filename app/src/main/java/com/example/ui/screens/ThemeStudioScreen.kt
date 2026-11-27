package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.MinimalCard
import com.example.ui.components.ProgressRing
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.AvailableAccents
import com.example.ui.theme.LocalSyllabyColors
import com.example.ui.theme.ThemeSettings
import com.example.ui.theme.ThemeVariant
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.resolveSyllabyColors

@Composable
fun ThemeStudioScreen(
    themeSettings: ThemeSettings,
    onUpdateTheme: (ThemeSettings) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalSyllabyColors.current
    BackHandler { onBack() }

    val corePresets = AppThemePreset.entries.filter { !it.isSeasonal }
    val seasonalPresets = AppThemePreset.entries.filter { it.isSeasonal }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("theme_studio_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Theme Studio",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Instant OLED, daylight & seasonal atmospheres",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            // Live Preview Card
            item {
                MinimalCard(highlightBorder = colors.accent.copy(alpha = 0.45f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LIVE PREVIEW • ${themeSettings.preset.displayName.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Calculus & Linear Algebra",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { 0.72f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = colors.accent,
                                trackColor = colors.borderSubtle
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        ProgressRing(progress = 0.72f, size = 56.dp, strokeWidth = 5.dp)
                    }
                }
            }

            // Dark / Light variant + Seasonal FX
            item {
                MinimalCard {
                    if (themeSettings.preset.supportsLightVariant) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (themeSettings.variant == ThemeVariant.DARK) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                                    contentDescription = null,
                                    tint = colors.accent
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Dark Variant", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Text("Switch between dark & light atmospheric tones", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                            }
                            Switch(
                                checked = themeSettings.variant == ThemeVariant.DARK,
                                onCheckedChange = { dark ->
                                    onUpdateTheme(themeSettings.copy(variant = if (dark) ThemeVariant.DARK else ThemeVariant.LIGHT))
                                },
                                modifier = Modifier.testTag("theme_variant_switch")
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Palette, contentDescription = null, tint = colors.accent)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Seasonal Atmospheric Details", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Text("Subtle frost, petal, solar, and ember top accents", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        }
                        Switch(
                            checked = themeSettings.seasonalAtmosphereEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateTheme(themeSettings.copy(seasonalAtmosphereEnabled = enabled))
                            },
                            modifier = Modifier.testTag("seasonal_fx_switch")
                        )
                    }
                }
            }

            // Custom Accent Swatches
            item {
                MinimalCard {
                    SectionHeader(title = "Custom Accent Highlight", subtitle = "Applies across buttons, rings & focus indicators")
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(AvailableAccents, key = { it.id }) { accent ->
                            val selected = themeSettings.customAccentHex == accent.hex
                            val swatchColor = if (accent.hex.isBlank()) {
                                resolveSyllabyColors(themeSettings.copy(customAccentHex = ""), colors.isDark).accent
                            } else {
                                parseHexColor(accent.hex, colors.accent)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onUpdateTheme(themeSettings.copy(customAccentHex = accent.hex)) }
                                    .padding(6.dp)
                                    .testTag("accent_swatch_${accent.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) colors.textPrimary else colors.borderSubtle,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Selected",
                                            tint = colors.background,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(accent.label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = "Core Themes", subtitle = "OLED, Daylight, Rose Quartz, Navy & System") }
            items(corePresets, key = { it.name }) { preset ->
                ThemePresetCard(
                    preset = preset,
                    selected = themeSettings.preset == preset,
                    variant = themeSettings.variant,
                    customAccentHex = themeSettings.customAccentHex,
                    onSelect = { onUpdateTheme(themeSettings.copy(preset = preset)) }
                )
            }

            item { SectionHeader(title = "Seasonal Themes", subtitle = "Winter, Spring, Summer & Autumn palettes") }
            items(seasonalPresets, key = { it.name }) { preset ->
                ThemePresetCard(
                    preset = preset,
                    selected = themeSettings.preset == preset,
                    variant = themeSettings.variant,
                    customAccentHex = themeSettings.customAccentHex,
                    onSelect = { onUpdateTheme(themeSettings.copy(preset = preset)) }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ThemePresetCard(
    preset: AppThemePreset,
    selected: Boolean,
    variant: ThemeVariant,
    customAccentHex: String,
    onSelect: () -> Unit
) {
    val colors = LocalSyllabyColors.current
    val previewPalette = resolveSyllabyColors(
        settings = ThemeSettings(
            preset = preset,
            variant = variant,
            customAccentHex = customAccentHex
        ),
        systemDark = colors.isDark
    )

    Surface(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("theme_preset_${preset.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) colors.accent else colors.borderSubtle
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Mini palette preview chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(previewPalette.background)
                        .border(1.dp, previewPalette.borderSubtle, RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(previewPalette.surfaceElevated))
                    Box(Modifier.size(14.dp).clip(CircleShape).background(previewPalette.accent))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(preset.displayName, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    Text(preset.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
            if (selected) {
                Icon(Icons.Outlined.Check, contentDescription = "Active theme", tint = colors.accent)
            }
        }
    }
}
