package com.kelsos.mbrc.features.theme

import androidx.appcompat.app.AppCompatDelegate
import com.kelsos.mbrc.features.settings.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ThemeManagerImpl(private val settingsManager: SettingsManager) : ThemeManager {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  init {
    // Reactively observe theme changes and apply them
    scope.launch {
      settingsManager.themeFlow.collect { theme ->
        applyThemeMode(theme)
      }
    }
  }

  override fun applyTheme() {
    scope.launch {
      val themePreference = settingsManager.themeFlow.first()
      applyThemeMode(themePreference)
    }
  }

  override suspend fun applyTheme(theme: Theme) {
    settingsManager.setTheme(theme)
    // Theme will be applied automatically via the reactive flow
  }

  private fun applyThemeMode(theme: Theme) {
    val nightMode = when (theme) {
      Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
      Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
      Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
    AppCompatDelegate.setDefaultNightMode(nightMode)
  }
}

interface ThemeManager {
  fun applyTheme()

  suspend fun applyTheme(theme: Theme)
}
