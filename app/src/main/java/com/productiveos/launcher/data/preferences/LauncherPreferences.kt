package com.productiveos.launcher.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "productive_os_prefs")

enum class TextAlignmentOption(val title: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

enum class TypographyStyleOption(val title: String) {
    CLEAN_SANS("Clean Sans"),
    MONOSPACE_TERMINAL("Monospace Terminal"),
    EDITORIAL_SERIF("Editorial Serif")
}

enum class LauncherThemeOption(val title: String, val hexLabel: String) {
    OLED_BLACK("Pure OLED Black", "#000000"),
    SLATE_GRAY("Slate Gray", "#121212"),
    WARM_STONE("Warm Stone", "#F5F5F0"),
    PALE_SAGE("Pale Sage", "#EAEFE9"),
    TOKYO_MIST("Tokyo Mist", "#E9EEF3"),
    OCHRE_SAND("Ochre Sand", "#F5EFE6"),
    MIDNIGHT_NAVY("Midnight Navy", "#0B101B");

    val isLight: Boolean
        get() = this == WARM_STONE || this == PALE_SAGE || this == TOKYO_MIST || this == OCHRE_SAND
}

data class LauncherSettings(
    val textAlignment: TextAlignmentOption = TextAlignmentOption.LEFT,
    val typographyStyle: TypographyStyleOption = TypographyStyleOption.CLEAN_SANS,
    val themeOption: LauncherThemeOption = LauncherThemeOption.OLED_BLACK,
    val fontScale: Float = 1.0f,
    val autoLaunchSingleResult: Boolean = true,
    val enableT9Search: Boolean = true,
    val frictionSeconds: Int = 3,
    val mindfulFrictionEnabled: Boolean = true,
    val enableHoldToOpen: Boolean = true,
    val holdToOpenSeconds: Int = 3,
    val installedWidgetIds: List<Int> = emptyList(),
    val vaultPasscode: String = "",
    val hasInitializedDefaults: Boolean = false,
    val statusBarStealth: Boolean = false,
    val bottomDockedSearch: Boolean = false,
    val swipeDownForNotifications: Boolean = true,
    val omniSearchToolsEnabled: Boolean = true,
    val autoDayNightTheme: Boolean = false,
    val canvasSwipeLeftPkg: String = "",
    val canvasSwipeRightPkg: String = "",
    val burnerNoteText: String = "",
    val burnerNoteUpdatedAt: Long = 0L
)

@Singleton
class LauncherPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TEXT_ALIGNMENT = stringPreferencesKey("text_alignment")
        val TYPOGRAPHY_STYLE = stringPreferencesKey("typography_style")
        val THEME_OPTION = stringPreferencesKey("theme_option")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val AUTO_LAUNCH_SINGLE = booleanPreferencesKey("auto_launch_single")
        val ENABLE_T9_SEARCH = booleanPreferencesKey("enable_t9_search")
        val FRICTION_SECONDS = intPreferencesKey("friction_seconds")
        val MINDFUL_FRICTION_ENABLED = booleanPreferencesKey("mindful_friction_enabled")
        val ENABLE_HOLD_TO_OPEN = booleanPreferencesKey("enable_hold_to_open")
        val HOLD_TO_OPEN_SECONDS = intPreferencesKey("hold_to_open_seconds")
        val INSTALLED_WIDGET_IDS = stringPreferencesKey("installed_widget_ids")
        val VAULT_PASSCODE = stringPreferencesKey("vault_passcode")
        val HAS_INITIALIZED_DEFAULTS = booleanPreferencesKey("has_initialized_defaults")
        val STATUS_BAR_STEALTH = booleanPreferencesKey("status_bar_stealth")
        val BOTTOM_DOCKED_SEARCH = booleanPreferencesKey("bottom_docked_search")
        val SWIPE_DOWN_NOTIFICATIONS = booleanPreferencesKey("swipe_down_notifications")
        val OMNI_SEARCH_TOOLS = booleanPreferencesKey("omni_search_tools")
        val AUTO_DAY_NIGHT_THEME = booleanPreferencesKey("auto_day_night_theme")
        val CANVAS_SWIPE_LEFT_PKG = stringPreferencesKey("canvas_swipe_left_pkg")
        val CANVAS_SWIPE_RIGHT_PKG = stringPreferencesKey("canvas_swipe_right_pkg")
        val BURNER_NOTE_TEXT = stringPreferencesKey("burner_note_text")
        val BURNER_NOTE_UPDATED_AT = longPreferencesKey("burner_note_updated_at")
    }

    val settingsFlow: Flow<LauncherSettings> = context.dataStore.data.map { prefs ->
        val alignment = prefs[Keys.TEXT_ALIGNMENT]?.let {
            runCatching { TextAlignmentOption.valueOf(it) }.getOrNull()
        } ?: TextAlignmentOption.LEFT

        val typography = prefs[Keys.TYPOGRAPHY_STYLE]?.let {
            runCatching { TypographyStyleOption.valueOf(it) }.getOrNull()
        } ?: TypographyStyleOption.CLEAN_SANS

        val theme = prefs[Keys.THEME_OPTION]?.let {
            runCatching { LauncherThemeOption.valueOf(it) }.getOrNull()
        } ?: LauncherThemeOption.OLED_BLACK

        val widgetIds = prefs[Keys.INSTALLED_WIDGET_IDS]?.let { raw ->
            raw.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
        } ?: emptyList()

        LauncherSettings(
            textAlignment = alignment,
            typographyStyle = typography,
            themeOption = theme,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            autoLaunchSingleResult = prefs[Keys.AUTO_LAUNCH_SINGLE] ?: true,
            enableT9Search = prefs[Keys.ENABLE_T9_SEARCH] ?: true,
            frictionSeconds = prefs[Keys.FRICTION_SECONDS] ?: 3,
            mindfulFrictionEnabled = prefs[Keys.MINDFUL_FRICTION_ENABLED] ?: true,
            enableHoldToOpen = prefs[Keys.ENABLE_HOLD_TO_OPEN] ?: true,
            holdToOpenSeconds = prefs[Keys.HOLD_TO_OPEN_SECONDS] ?: 3,
            installedWidgetIds = widgetIds,
            vaultPasscode = prefs[Keys.VAULT_PASSCODE] ?: "",
            hasInitializedDefaults = prefs[Keys.HAS_INITIALIZED_DEFAULTS] ?: false,
            statusBarStealth = prefs[Keys.STATUS_BAR_STEALTH] ?: false,
            bottomDockedSearch = prefs[Keys.BOTTOM_DOCKED_SEARCH] ?: false,
            swipeDownForNotifications = prefs[Keys.SWIPE_DOWN_NOTIFICATIONS] ?: true,
            omniSearchToolsEnabled = prefs[Keys.OMNI_SEARCH_TOOLS] ?: true,
            autoDayNightTheme = prefs[Keys.AUTO_DAY_NIGHT_THEME] ?: false,
            canvasSwipeLeftPkg = prefs[Keys.CANVAS_SWIPE_LEFT_PKG] ?: "",
            canvasSwipeRightPkg = prefs[Keys.CANVAS_SWIPE_RIGHT_PKG] ?: "",
            burnerNoteText = prefs[Keys.BURNER_NOTE_TEXT] ?: "",
            burnerNoteUpdatedAt = prefs[Keys.BURNER_NOTE_UPDATED_AT] ?: 0L
        )
    }

    suspend fun setTextAlignment(option: TextAlignmentOption) {
        context.dataStore.edit { it[Keys.TEXT_ALIGNMENT] = option.name }
    }

    suspend fun setTypographyStyle(option: TypographyStyleOption) {
        context.dataStore.edit { it[Keys.TYPOGRAPHY_STYLE] = option.name }
    }

    suspend fun setThemeOption(option: LauncherThemeOption) {
        context.dataStore.edit { it[Keys.THEME_OPTION] = option.name }
    }

    suspend fun toggleQuickContrastTheme() {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.THEME_OPTION]?.let {
                runCatching { LauncherThemeOption.valueOf(it) }.getOrNull()
            } ?: LauncherThemeOption.OLED_BLACK
            val next = when (current) {
                LauncherThemeOption.OLED_BLACK -> LauncherThemeOption.WARM_STONE
                LauncherThemeOption.SLATE_GRAY -> LauncherThemeOption.OLED_BLACK
                LauncherThemeOption.WARM_STONE -> LauncherThemeOption.OLED_BLACK
            }
            prefs[Keys.THEME_OPTION] = next.name
        }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.30f) }
    }

    suspend fun setAutoLaunchSingleResult(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_LAUNCH_SINGLE] = enabled }
    }

    suspend fun setEnableT9Search(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENABLE_T9_SEARCH] = enabled }
    }

    suspend fun setFrictionSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.FRICTION_SECONDS] = seconds.coerceIn(3, 15) }
    }

    suspend fun setMindfulFrictionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MINDFUL_FRICTION_ENABLED] = enabled }
    }

    suspend fun setEnableHoldToOpen(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENABLE_HOLD_TO_OPEN] = enabled }
    }

    suspend fun setHoldToOpenSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.HOLD_TO_OPEN_SECONDS] = seconds.coerceIn(1, 5) }
    }

    suspend fun addInstalledWidgetId(widgetId: Int) {
        context.dataStore.edit { prefs ->
            val existing = prefs[Keys.INSTALLED_WIDGET_IDS]?.let { raw ->
                raw.split(",").mapNotNull { it.trim().toIntOrNull() }
            } ?: emptyList()
            if (!existing.contains(widgetId)) {
                prefs[Keys.INSTALLED_WIDGET_IDS] = (existing + widgetId).joinToString(",")
            }
        }
    }

    suspend fun removeInstalledWidgetId(widgetId: Int) {
        context.dataStore.edit { prefs ->
            val existing = prefs[Keys.INSTALLED_WIDGET_IDS]?.let { raw ->
                raw.split(",").mapNotNull { it.trim().toIntOrNull() }
            } ?: emptyList()
            prefs[Keys.INSTALLED_WIDGET_IDS] = existing.filter { it != widgetId }.joinToString(",")
        }
    }

    suspend fun setVaultPasscode(passcode: String) {
        context.dataStore.edit { it[Keys.VAULT_PASSCODE] = passcode }
    }

    suspend fun setStatusBarStealth(stealth: Boolean) {
        context.dataStore.edit { it[Keys.STATUS_BAR_STEALTH] = stealth }
    }

    suspend fun setBottomDockedSearch(bottom: Boolean) {
        context.dataStore.edit { it[Keys.BOTTOM_DOCKED_SEARCH] = bottom }
    }

    suspend fun setSwipeDownForNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SWIPE_DOWN_NOTIFICATIONS] = enabled }
    }

    suspend fun setOmniSearchToolsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OMNI_SEARCH_TOOLS] = enabled }
    }

    suspend fun setAutoDayNightTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_DAY_NIGHT_THEME] = enabled }
    }

    suspend fun setCanvasSwipeLeftPkg(pkg: String) {
        context.dataStore.edit { it[Keys.CANVAS_SWIPE_LEFT_PKG] = pkg }
    }

    suspend fun setCanvasSwipeRightPkg(pkg: String) {
        context.dataStore.edit { it[Keys.CANVAS_SWIPE_RIGHT_PKG] = pkg }
    }

    suspend fun updateBurnerNote(text: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BURNER_NOTE_TEXT] = text
            prefs[Keys.BURNER_NOTE_UPDATED_AT] = if (text.isBlank()) 0L else System.currentTimeMillis()
        }
    }

    suspend fun purgeExpiredBurnerNoteIfNeeded() {
        context.dataStore.edit { prefs ->
            val updatedAt = prefs[Keys.BURNER_NOTE_UPDATED_AT] ?: 0L
            val now = System.currentTimeMillis()
            val twentyFourHoursMs = 24 * 3600_000L
            if (updatedAt > 0L && (now - updatedAt) >= twentyFourHoursMs) {
                prefs[Keys.BURNER_NOTE_TEXT] = ""
                prefs[Keys.BURNER_NOTE_UPDATED_AT] = 0L
            }
        }
    }

    suspend fun markDefaultsInitialized() {
        context.dataStore.edit { it[Keys.HAS_INITIALIZED_DEFAULTS] = true }
    }
}
