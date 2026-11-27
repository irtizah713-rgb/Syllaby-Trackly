package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DashboardWidgetId
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.ThemeSettings
import com.example.ui.theme.ThemeVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "syllaby_preferences")

data class FocusPreferences(
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val ambientSound: String = "Silent"
)

data class NotificationPreferences(
    val taskReminders: Boolean = true,
    val focusCompletion: Boolean = true,
    val streakAlerts: Boolean = true,
    val achievementAlerts: Boolean = true
)

data class DashboardLayoutPreferences(
    val orderedWidgets: List<DashboardWidgetId> = DashboardWidgetId.entries,
    val hiddenWidgets: Set<DashboardWidgetId> = emptySet()
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val THEME_VARIANT = stringPreferencesKey("theme_variant")
        val CUSTOM_ACCENT = stringPreferencesKey("custom_accent_hex")
        val SEASONAL_FX = booleanPreferencesKey("seasonal_atmosphere")

        val FOCUS_MINUTES = intPreferencesKey("focus_minutes")
        val BREAK_MINUTES = intPreferencesKey("break_minutes")
        val LONG_BREAK_MINUTES = intPreferencesKey("long_break_minutes")
        val AMBIENT_SOUND = stringPreferencesKey("ambient_sound")

        val NOTIF_TASKS = booleanPreferencesKey("notif_tasks")
        val NOTIF_FOCUS = booleanPreferencesKey("notif_focus")
        val NOTIF_STREAK = booleanPreferencesKey("notif_streak")
        val NOTIF_ACHIEVEMENT = booleanPreferencesKey("notif_achievement")

        val WIDGET_ORDER = stringPreferencesKey("dashboard_widget_order")
        val WIDGET_HIDDEN = stringPreferencesKey("dashboard_widget_hidden")

        val LAST_BACKUP_MILLIS = longPreferencesKey("last_backup_millis")
        val AUTO_BACKUP_JSON = stringPreferencesKey("auto_backup_json")
    }

    val themeSettingsFlow: Flow<ThemeSettings> = context.dataStore.data.map { prefs ->
        val presetName = prefs[Keys.THEME_PRESET]
        val preset = AppThemePreset.entries.firstOrNull { it.name == presetName } ?: AppThemePreset.OLED_BLACK
        val variantName = prefs[Keys.THEME_VARIANT]
        val variant = ThemeVariant.entries.firstOrNull { it.name == variantName } ?: ThemeVariant.DARK
        ThemeSettings(
            preset = preset,
            variant = variant,
            customAccentHex = prefs[Keys.CUSTOM_ACCENT] ?: "",
            seasonalAtmosphereEnabled = prefs[Keys.SEASONAL_FX] ?: true
        )
    }

    val focusPreferencesFlow: Flow<FocusPreferences> = context.dataStore.data.map { prefs ->
        FocusPreferences(
            focusMinutes = (prefs[Keys.FOCUS_MINUTES] ?: 25).coerceIn(1, 180),
            breakMinutes = (prefs[Keys.BREAK_MINUTES] ?: 5).coerceIn(1, 60),
            longBreakMinutes = (prefs[Keys.LONG_BREAK_MINUTES] ?: 15).coerceIn(5, 90),
            ambientSound = prefs[Keys.AMBIENT_SOUND] ?: "Silent"
        )
    }

    val notificationPreferencesFlow: Flow<NotificationPreferences> = context.dataStore.data.map { prefs ->
        NotificationPreferences(
            taskReminders = prefs[Keys.NOTIF_TASKS] ?: true,
            focusCompletion = prefs[Keys.NOTIF_FOCUS] ?: true,
            streakAlerts = prefs[Keys.NOTIF_STREAK] ?: true,
            achievementAlerts = prefs[Keys.NOTIF_ACHIEVEMENT] ?: true
        )
    }

    val dashboardLayoutFlow: Flow<DashboardLayoutPreferences> = context.dataStore.data.map { prefs ->
        val savedOrder = prefs[Keys.WIDGET_ORDER]
            ?.split(",")
            ?.mapNotNull { name -> DashboardWidgetId.entries.firstOrNull { it.name == name } }
            ?.distinct()
            .orEmpty()
        val fullOrder = (savedOrder + DashboardWidgetId.entries).distinct()
        val hidden = prefs[Keys.WIDGET_HIDDEN]
            ?.split(",")
            ?.mapNotNull { name -> DashboardWidgetId.entries.firstOrNull { it.name == name } }
            ?.toSet()
            ?: emptySet()
        DashboardLayoutPreferences(orderedWidgets = fullOrder, hiddenWidgets = hidden)
    }

    val lastBackupMillisFlow: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_BACKUP_MILLIS] ?: 0L }
    val autoBackupJsonFlow: Flow<String> = context.dataStore.data.map { it[Keys.AUTO_BACKUP_JSON] ?: "" }

    suspend fun updateThemeSettings(settings: ThemeSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_PRESET] = settings.preset.name
            prefs[Keys.THEME_VARIANT] = settings.variant.name
            prefs[Keys.CUSTOM_ACCENT] = settings.customAccentHex
            prefs[Keys.SEASONAL_FX] = settings.seasonalAtmosphereEnabled
        }
    }

    suspend fun updateFocusPreferences(focus: FocusPreferences) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FOCUS_MINUTES] = focus.focusMinutes
            prefs[Keys.BREAK_MINUTES] = focus.breakMinutes
            prefs[Keys.LONG_BREAK_MINUTES] = focus.longBreakMinutes
            prefs[Keys.AMBIENT_SOUND] = focus.ambientSound
        }
    }

    suspend fun updateNotificationPreferences(notif: NotificationPreferences) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIF_TASKS] = notif.taskReminders
            prefs[Keys.NOTIF_FOCUS] = notif.focusCompletion
            prefs[Keys.NOTIF_STREAK] = notif.streakAlerts
            prefs[Keys.NOTIF_ACHIEVEMENT] = notif.achievementAlerts
        }
    }

    suspend fun updateDashboardLayout(ordered: List<DashboardWidgetId>, hidden: Set<DashboardWidgetId>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WIDGET_ORDER] = ordered.joinToString(",") { it.name }
            prefs[Keys.WIDGET_HIDDEN] = hidden.joinToString(",") { it.name }
        }
    }

    suspend fun saveLocalBackupSnapshot(json: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_BACKUP_JSON] = json
            prefs[Keys.LAST_BACKUP_MILLIS] = System.currentTimeMillis()
        }
    }
}
