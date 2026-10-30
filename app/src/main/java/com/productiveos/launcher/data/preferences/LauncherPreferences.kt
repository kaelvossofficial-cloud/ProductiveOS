package com.productiveos.launcher.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
    WARM_STONE("Warm Stone", "#F5F5F0")
}

data class LauncherSettings(
    val textAlignment: TextAlignmentOption = TextAlignmentOption.LEFT,
    val typographyStyle: TypographyStyleOption = TypographyStyleOption.CLEAN_SANS,
    val themeOption: LauncherThemeOption = LauncherThemeOption.OLED_BLACK,
    val fontScale: Float = 1.0f,
    val autoLaunchSingleResult: Boolean = true,
    val enableT9Search: Boolean = true,
    val frictionSeconds: Int = 5,
    val vaultPasscode: String = "",
    val hasInitializedDefaults: Boolean = false
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
        val VAULT_PASSCODE = stringPreferencesKey("vault_passcode")
        val HAS_INITIALIZED_DEFAULTS = booleanPreferencesKey("has_initialized_defaults")
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

        LauncherSettings(
            textAlignment = alignment,
            typographyStyle = typography,
            themeOption = theme,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            autoLaunchSingleResult = prefs[Keys.AUTO_LAUNCH_SINGLE] ?: true,
            enableT9Search = prefs[Keys.ENABLE_T9_SEARCH] ?: true,
            frictionSeconds = prefs[Keys.FRICTION_SECONDS] ?: 5,
            vaultPasscode = prefs[Keys.VAULT_PASSCODE] ?: "",
            hasInitializedDefaults = prefs[Keys.HAS_INITIALIZED_DEFAULTS] ?: false
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

    suspend fun setVaultPasscode(passcode: String) {
        context.dataStore.edit { it[Keys.VAULT_PASSCODE] = passcode }
    }

    suspend fun markDefaultsInitialized() {
        context.dataStore.edit { it[Keys.HAS_INITIALIZED_DEFAULTS] = true }
    }
}

