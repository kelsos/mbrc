package com.kelsos.mbrc.feature.settings.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.feature.settings.domain.SettingsManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The manager applies the theme from a flow it starts collecting in its constructor, so each test
 * seeds [theme] before building one. The night mode it sets is process-wide state, which [tearDown]
 * puts back so an ordering change cannot make another test pass or fail.
 */
@RunWith(AndroidJUnit4::class)
class ThemeManagerImplTest {

  private lateinit var settingsManager: SettingsManager
  private lateinit var theme: MutableStateFlow<Theme>

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    theme = MutableStateFlow<Theme>(Theme.System)
    settingsManager = mockk(relaxed = true) {
      every { themeFlow } returns theme
    }
  }

  @After
  fun tearDown() {
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    Dispatchers.resetMain()
  }

  @Test
  fun `the light theme turns night mode off`() {
    theme.value = Theme.Light

    ThemeManagerImpl(settingsManager)

    assertThat(AppCompatDelegate.getDefaultNightMode())
      .isEqualTo(AppCompatDelegate.MODE_NIGHT_NO)
  }

  @Test
  fun `the dark theme turns night mode on`() {
    theme.value = Theme.Dark

    ThemeManagerImpl(settingsManager)

    assertThat(AppCompatDelegate.getDefaultNightMode())
      .isEqualTo(AppCompatDelegate.MODE_NIGHT_YES)
  }

  @Test
  fun `the system theme follows the system`() {
    theme.value = Theme.Dark
    ThemeManagerImpl(settingsManager)

    theme.value = Theme.System

    assertThat(AppCompatDelegate.getDefaultNightMode())
      .isEqualTo(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
  }

  @Test
  fun `a stored theme change is applied without anyone asking for it`() {
    theme.value = Theme.Light
    ThemeManagerImpl(settingsManager)

    theme.value = Theme.Dark

    assertWithMessage("the manager collects the flow, so a setting written elsewhere still applies")
      .that(AppCompatDelegate.getDefaultNightMode())
      .isEqualTo(AppCompatDelegate.MODE_NIGHT_YES)
  }

  @Test
  fun `choosing a theme is written through the settings manager`() = runTest {
    val manager = ThemeManagerImpl(settingsManager)

    manager.applyTheme(Theme.Dark)

    coVerify { settingsManager.setTheme(Theme.Dark) }
  }

  @Test
  fun `applying on demand reads the theme back from the settings`() {
    theme.value = Theme.Dark
    val manager = ThemeManagerImpl(settingsManager)
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

    manager.applyTheme()

    assertThat(AppCompatDelegate.getDefaultNightMode())
      .isEqualTo(AppCompatDelegate.MODE_NIGHT_YES)
  }
}
