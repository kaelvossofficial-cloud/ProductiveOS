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

enum class TypographyStyleOption(val title: String, val subtitle: String = "") {
    CLEAN_SANS("Clean Sans", "Modern geometric sans-serif"),
    MONOSPACE_TERMINAL("Monospace", "Precision technical code type"),
    EDITORIAL_SERIF("Editorial Serif", "Classic book & magazine serif")
}

enum class LauncherThemeOption(
    val title: String,
    val subtitle: String,
    val hexLabel: String = ""
) {
    LIQUID_FROST("Liquid Frost", "Sapphire glass & cyan luminescence", "#070D19"),
    AURORA_EMERALD("Aurora Emerald", "Forest obsidian & bioluminescent mint", "#06120E"),
    COSMIC_VIOLET("Cosmic Amethyst", "Velvet space & neon lavender glass", "#0C0818"),
    SUNSET_EMBER("Sunset Coral", "Warm obsidian & solar coral glow", "#160A08"),
    OCEAN_ABYSS("Oceanic Cyan", "Abyssal teal & aqua pearl glass", "#051118"),
    OLED_BLACK("Obsidian OLED", "Zero-power pitch black & ice rim", "#000000"),
    MIDNIGHT_NAVY("Midnight Navy", "Deep slate blue & sky cobalt glass", "#0B101B"),
    SLATE_GRAY("Carbon Slate", "Matte titanium & silver frost", "#121212"),
    ROSE_QUARTZ("Rose Quartz", "Daylight blush pearl & berry glass", "#F9F1F4"),
    WARM_STONE("Warm Stone", "Editorial parchment & bronze glass", "#F5F5F0"),
    PALE_SAGE("Botanical Sage", "Daylight eucalyptus & jade glass", "#EAEFE9"),
    TOKYO_MIST("Tokyo Mist", "Cool daylight mist & indigo glass", "#E9EEF3"),
    OCHRE_SAND("Ochre Sand", "Desert dune & terracotta glass", "#F5EFE6");

    val isLight: Boolean
        get() = this == ROSE_QUARTZ || this == WARM_STONE || this == PALE_SAGE || this == TOKYO_MIST || this == OCHRE_SAND
}

enum class VolumeBarStyleOption(val title: String, val subtitle: String) {
    LIQUID_GLASS_PILL("Liquid Glass Capsule", "Frosted specular pill with glowing liquid fill"),
    EQUALIZER_WAVE("Voice Equalizer Wave", "Dynamic 18-bar acoustic voice waveform"),
    NEON_ORBIT_ARC("Neon Tactile Matrix", "Segmented glowing squircle cells with pulse"),
    MINIMAL_EDGE_BLADE("Minimal Edge Blade", "Sleek glass rail with floating luminescence orb")
}

enum class IconColorMode(val label: String) {
    THEME_MONOCHROME("Theme Harmonized"),
    ORIGINAL_COLOR("Original Colors")
}

data class FavoriteContact(val name: String, val number: String)

data class LauncherSettings(
    val textAlignment: TextAlignmentOption = TextAlignmentOption.LEFT,
    val typographyStyle: TypographyStyleOption = TypographyStyleOption.CLEAN_SANS,
    val themeOption: LauncherThemeOption = LauncherThemeOption.LIQUID_FROST,
    val fontScale: Float = 1.0f,
    val iconScale: Float = 1.0f,
    val iconColorMode: IconColorMode = IconColorMode.THEME_MONOCHROME,
    val hasCompletedOnboarding: Boolean = false,
    val autoLaunchSingleResult: Boolean = true,
    val enableT9Search: Boolean = true,
    val frictionSeconds: Int = 3,
    val mindfulFrictionEnabled: Boolean = true,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showTelemetry: Boolean = true,
    val showWhisper: Boolean = true,
    val showMusicCapsule: Boolean = true,
    val showIconDock: Boolean = true,
    val doubleTapToLock: Boolean = false,
    val showInteractiveGrid: Boolean = true,
    val showHomeEdgeSidebar: Boolean = true,
    val floatingHomeIcons: Boolean = true,
    val drawerGridMode: Boolean = false,
    val favoriteContacts: List<FavoriteContact> = emptyList(),
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
    val burnerNoteUpdatedAt: Long = 0L,
    val volumeBarStyle: VolumeBarStyleOption = VolumeBarStyleOption.LIQUID_GLASS_PILL,
    val customVolumeBarEnabled: Boolean = true
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
        val ICON_SCALE = floatPreferencesKey("icon_scale")
        val ICON_COLOR_MODE = stringPreferencesKey("icon_color_mode")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val AUTO_LAUNCH_SINGLE = booleanPreferencesKey("auto_launch_single")
        val ENABLE_T9_SEARCH = booleanPreferencesKey("enable_t9_search")
        val FRICTION_SECONDS = intPreferencesKey("friction_seconds")
        val MINDFUL_FRICTION_ENABLED = booleanPreferencesKey("mindful_friction_enabled")
        val SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val SHOW_TELEMETRY = booleanPreferencesKey("show_telemetry")
        val SHOW_WHISPER = booleanPreferencesKey("show_whisper")
        val SHOW_MUSIC_CAPSULE = booleanPreferencesKey("show_music_capsule")
        val SHOW_ICON_DOCK = booleanPreferencesKey("show_icon_dock")
        val DOUBLE_TAP_TO_LOCK = booleanPreferencesKey("double_tap_to_lock")
        val SHOW_INTERACTIVE_GRID = booleanPreferencesKey("show_interactive_grid")
        val SHOW_HOME_EDGE_SIDEBAR = booleanPreferencesKey("show_home_edge_sidebar")
        val FLOATING_HOME_ICONS = booleanPreferencesKey("floating_home_icons")
        val DRAWER_GRID_MODE = booleanPreferencesKey("drawer_grid_mode")
        val FAVORITE_CONTACTS = stringPreferencesKey("favorite_contacts")
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
        val VOLUME_BAR_STYLE = stringPreferencesKey("volume_bar_style")
        val CUSTOM_VOLUME_BAR_ENABLED = booleanPreferencesKey("custom_volume_bar_enabled")
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
        } ?: LauncherThemeOption.LIQUID_FROST

        val volumeStyle = prefs[Keys.VOLUME_BAR_STYLE]?.let {
            runCatching { VolumeBarStyleOption.valueOf(it) }.getOrNull()
        } ?: VolumeBarStyleOption.LIQUID_GLASS_PILL

        val widgetIds = prefs[Keys.INSTALLED_WIDGET_IDS]?.let { raw ->
            raw.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
        } ?: emptyList()

        LauncherSettings(
            textAlignment = alignment,
            typographyStyle = typography,
            themeOption = theme,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            iconScale = prefs[Keys.ICON_SCALE] ?: 1.0f,
            iconColorMode = prefs[Keys.ICON_COLOR_MODE]?.let { runCatching { IconColorMode.valueOf(it) }.getOrNull() } ?: IconColorMode.THEME_MONOCHROME,
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
            autoLaunchSingleResult = prefs[Keys.AUTO_LAUNCH_SINGLE] ?: true,
            enableT9Search = prefs[Keys.ENABLE_T9_SEARCH] ?: true,
            frictionSeconds = prefs[Keys.FRICTION_SECONDS] ?: 3,
            mindfulFrictionEnabled = prefs[Keys.MINDFUL_FRICTION_ENABLED] ?: true,
            showClock = prefs[Keys.SHOW_CLOCK] ?: true,
            showDate = prefs[Keys.SHOW_DATE] ?: true,
            showTelemetry = prefs[Keys.SHOW_TELEMETRY] ?: true,
            showWhisper = prefs[Keys.SHOW_WHISPER] ?: true,
            showMusicCapsule = prefs[Keys.SHOW_MUSIC_CAPSULE] ?: true,
            showIconDock = prefs[Keys.SHOW_ICON_DOCK] ?: true,
            doubleTapToLock = prefs[Keys.DOUBLE_TAP_TO_LOCK] ?: false,
            showInteractiveGrid = prefs[Keys.SHOW_INTERACTIVE_GRID] ?: true,
            showHomeEdgeSidebar = prefs[Keys.SHOW_HOME_EDGE_SIDEBAR] ?: true,
            floatingHomeIcons = prefs[Keys.FLOATING_HOME_ICONS] ?: true,
            drawerGridMode = prefs[Keys.DRAWER_GRID_MODE] ?: false,
            favoriteContacts = decodeFavorites(prefs[Keys.FAVORITE_CONTACTS]),
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
            burnerNoteUpdatedAt = prefs[Keys.BURNER_NOTE_UPDATED_AT] ?: 0L,
            volumeBarStyle = volumeStyle,
            customVolumeBarEnabled = prefs[Keys.CUSTOM_VOLUME_BAR_ENABLED] ?: true
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

    suspend fun setVolumeBarStyle(option: VolumeBarStyleOption) {
        context.dataStore.edit { it[Keys.VOLUME_BAR_STYLE] = option.name }
    }

    suspend fun setCustomVolumeBarEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CUSTOM_VOLUME_BAR_ENABLED] = enabled }
    }

    suspend fun toggleQuickContrastTheme() {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.THEME_OPTION]?.let {
                runCatching { LauncherThemeOption.valueOf(it) }.getOrNull()
            } ?: LauncherThemeOption.LIQUID_FROST
            val next = if (current.isLight) {
                LauncherThemeOption.LIQUID_FROST
            } else {
                LauncherThemeOption.WARM_STONE
            }
            prefs[Keys.THEME_OPTION] = next.name
        }
    }

    suspend fun setIconScale(scale: Float) {
        context.dataStore.edit { it[Keys.ICON_SCALE] = scale.coerceIn(0.70f, 1.40f) }
    }

    suspend fun setIconColorMode(mode: IconColorMode) {
        context.dataStore.edit { it[Keys.ICON_COLOR_MODE] = mode.name }
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { it[Keys.HAS_COMPLETED_ONBOARDING] = completed }
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

    suspend fun setHomeToggle(name: String, value: Boolean) {
        val key = when (name) {
            "showClock" -> Keys.SHOW_CLOCK
            "showDate" -> Keys.SHOW_DATE
            "showTelemetry" -> Keys.SHOW_TELEMETRY
            "showWhisper" -> Keys.SHOW_WHISPER
            "showMusicCapsule" -> Keys.SHOW_MUSIC_CAPSULE
            "showIconDock" -> Keys.SHOW_ICON_DOCK
            "doubleTapToLock" -> Keys.DOUBLE_TAP_TO_LOCK
            "showInteractiveGrid" -> Keys.SHOW_INTERACTIVE_GRID
            "showHomeEdgeSidebar" -> Keys.SHOW_HOME_EDGE_SIDEBAR
            "floatingHomeIcons" -> Keys.FLOATING_HOME_ICONS
            "drawerGridMode" -> Keys.DRAWER_GRID_MODE
            "customVolumeBarEnabled" -> Keys.CUSTOM_VOLUME_BAR_ENABLED
            else -> return
        }
        context.dataStore.edit { it[key] = value }
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

    private fun decodeFavorites(raw: String?): List<FavoriteContact> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 2 && parts[1].isNotBlank()) FavoriteContact(parts[0], parts[1]) else null
        }
    }

    private fun encodeFavorites(list: List<FavoriteContact>): String =
        list.joinToString(";") { "${it.name.replace(";", " ").replace("|", " ")}|${it.number.filter { ch -> ch.isDigit() || ch == '+' }}" }

    suspend fun addFavoriteContact(name: String, number: String) {
        val cleanNumber = number.filter { it.isDigit() || it == '+' }
        if (cleanNumber.isBlank()) return
        context.dataStore.edit { prefs ->
            val existing = decodeFavorites(prefs[Keys.FAVORITE_CONTACTS])
            val label = name.trim().ifBlank { cleanNumber }
            prefs[Keys.FAVORITE_CONTACTS] = encodeFavorites((existing + FavoriteContact(label, cleanNumber)).takeLast(8))
        }
    }

    suspend fun removeFavoriteContact(index: Int) {
        context.dataStore.edit { prefs ->
            val existing = decodeFavorites(prefs[Keys.FAVORITE_CONTACTS]).toMutableList()
            if (index in existing.indices) existing.removeAt(index)
            prefs[Keys.FAVORITE_CONTACTS] = encodeFavorites(existing)
        }
    }

    suspend fun markDefaultsInitialized() {
        context.dataStore.edit { it[Keys.HAS_INITIALIZED_DEFAULTS] = true }
    }
}
