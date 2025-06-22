package com.kelsos.mbrc.features.theme

import androidx.appcompat.app.AppCompatDelegate
import com.kelsos.mbrc.features.settings.SettingsManager

class ThemeManagerImpl(
  private val settingsManager: SettingsManager,
) : ThemeManager {
  override fun applyTheme() {
    val themePreference = settingsManager.getThemePreference()
    val nightMode =
      when (themePreference) {
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      }
    AppCompatDelegate.setDefaultNightMode(nightMode)
  }

  override fun applyTheme(theme: String) {
    settingsManager.setThemePreference(theme)
    applyTheme()
  }
}

interface ThemeManager {
  fun applyTheme()

  fun applyTheme(theme: String)
}
