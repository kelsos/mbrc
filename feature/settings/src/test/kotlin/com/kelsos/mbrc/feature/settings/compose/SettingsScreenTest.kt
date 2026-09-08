package com.kelsos.mbrc.feature.settings.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.settings.TrackAction
import com.kelsos.mbrc.core.common.utilities.AppInfo
import com.kelsos.mbrc.feature.settings.SettingsDialogType
import com.kelsos.mbrc.feature.settings.data.CallAction
import com.kelsos.mbrc.feature.settings.theme.Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsScreenTest : KoinTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val testAppInfo = object : AppInfo {
    override val versionName: String = "1.2.3"
    override val versionCode: Int = 456
    override val buildTime: String = "2025-01-15T10:30:00Z"
    override val gitRevision: String = "abc123def"
    override val applicationId: String = "com.kelsos.mbrc.test"
  }

  private fun setupKoin() {
    startKoin {
      modules(
        module {
          single<AppInfo> { testAppInfo }
        }
      )
    }
  }

  private fun teardownKoin() {
    stopKoin()
  }

  // region Version Info Tests

  @Test
  fun `displays version number correctly`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("1.2.3", substring = true)
        .performScrollTo()
        .assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `displays build time correctly`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("2025-01-15T10:30:00Z")
        .performScrollTo()
        .assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `displays git revision correctly`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("abc123def")
        .performScrollTo()
        .assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  // endregion

  // region Theme Tests

  @Test
  fun `displays current theme as System`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(currentTheme = Theme.System),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Follow System").assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `displays current theme as Dark`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(currentTheme = Theme.Dark),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `displays current theme as Light`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(currentTheme = Theme.Light),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Light").assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `theme click triggers action`() {
    setupKoin()
    try {
      var themeClicked = false
      val actions = TestSettingsActions(onThemeClick = { themeClicked = true })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Theme").performClick()
      assertThat(themeClicked).isTrue()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `theme dialog is shown when visibleDialog is Theme`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(visibleDialog = SettingsDialogType.Theme),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      // Dialog should show theme options
      // "Follow System" appears in both settings item subtitle and dialog, so check we have at least 2
      composeTestRule.onAllNodesWithText("Follow System").fetchSemanticsNodes().size.let {
        assertThat(it).isAtLeast(2)
      }
      // Light and Dark are unique to the dialog
      composeTestRule.onNodeWithText("Light").assertIsDisplayed()
      composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `theme dialog selection triggers onThemeSelected`() {
    setupKoin()
    try {
      var selectedTheme: Theme? = null
      val actions = TestSettingsActions(onThemeSelected = { selectedTheme = it })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(
            currentTheme = Theme.System,
            visibleDialog = SettingsDialogType.Theme
          ),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      // Select Dark theme from dialog
      composeTestRule.onNodeWithText("Dark").performClick()
      assertThat(selectedTheme).isEqualTo(Theme.Dark)
    } finally {
      teardownKoin()
    }
  }

  // endregion

  // region Toggle Tests

  @Test
  fun `plugin updates toggle reflects enabled state`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(pluginUpdatesEnabled = true),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Plugin Update Check").assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `plugin updates toggle triggers action when clicked`() {
    setupKoin()
    try {
      var pluginUpdatesValue: Boolean? = null
      val actions = TestSettingsActions(onPluginUpdatesChanged = { pluginUpdatesValue = it })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(pluginUpdatesEnabled = false),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Plugin Update Check").performClick()
      assertThat(pluginUpdatesValue).isTrue()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `debug logging toggle triggers action when clicked`() {
    setupKoin()
    try {
      var debugLoggingValue: Boolean? = null
      val actions = TestSettingsActions(onDebugLoggingChanged = { debugLoggingValue = it })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(debugLoggingEnabled = false),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Debug Logging").performClick()
      assertThat(debugLoggingValue).isTrue()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `half star rating toggle triggers action`() {
    setupKoin()
    try {
      var halfStarValue: Boolean? = null
      val actions = TestSettingsActions(onHalfStarRatingChanged = { halfStarValue = it })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(halfStarRatingEnabled = true),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Half-star ratings").performClick()
      assertThat(halfStarValue).isFalse()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `show rating on player toggle triggers action`() {
    setupKoin()
    try {
      var showRatingValue: Boolean? = null
      val actions = TestSettingsActions(onShowRatingOnPlayerChanged = { showRatingValue = it })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(showRatingOnPlayerEnabled = false),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Show rating on player").performClick()
      assertThat(showRatingValue).isTrue()
    } finally {
      teardownKoin()
    }
  }

  // endregion

  // region Navigation Tests

  @Test
  fun `licenses click triggers navigation`() {
    setupKoin()
    try {
      var licensesClicked = false
      val actions = TestSettingsActions(onNavigateToLicenses = { licensesClicked = true })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Open source licenses")
        .performScrollTo()
        .performClick()
      assertThat(licensesClicked).isTrue()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `app license click triggers navigation`() {
    setupKoin()
    try {
      var appLicenseClicked = false
      val actions = TestSettingsActions(onNavigateToAppLicense = { appLicenseClicked = true })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("License")
        .performScrollTo()
        .performClick()
      assertThat(appLicenseClicked).isTrue()
    } finally {
      teardownKoin()
    }
  }

  // endregion

  // region Incoming Call Action Tests

  @Test
  fun `displays incoming call action as None`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(incomingCallAction = CallAction.None),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Do nothing").assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `displays incoming call action as Pause`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(incomingCallAction = CallAction.Pause),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Pause").assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `displays incoming call action as Reduce`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(incomingCallAction = CallAction.Reduce),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Reduce volume").assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `displays incoming call action as Stop`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(incomingCallAction = CallAction.Stop),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Stop").assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `incoming call action click triggers action`() {
    setupKoin()
    try {
      var callActionClicked = false
      val actions = TestSettingsActions(onIncomingCallActionClick = { callActionClicked = true })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Incoming call action").performClick()
      assertThat(callActionClicked).isTrue()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `incoming call action dialog shows all options`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(visibleDialog = SettingsDialogType.IncomingCallAction),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      // Dialog uses array resource values which differ from the settings item display
      // Check that all 4 options exist in the dialog (None appears in both subtitle and dialog)
      composeTestRule.onAllNodesWithText("None").fetchSemanticsNodes().size.let {
        assertThat(it).isAtLeast(1)
      }
      composeTestRule.onNodeWithText("Pause").assertIsDisplayed()
      composeTestRule.onNodeWithText("Stop").assertIsDisplayed()
      composeTestRule.onNodeWithText("Reduce Volume").assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `incoming call action dialog selection triggers onIncomingCallActionSelected`() {
    setupKoin()
    try {
      var selectedAction: CallAction? = null
      val actions = TestSettingsActions(onIncomingCallActionSelected = { selectedAction = it })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(
            incomingCallAction = CallAction.None,
            visibleDialog = SettingsDialogType.IncomingCallAction
          ),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Pause").performClick()
      assertThat(selectedAction).isEqualTo(CallAction.Pause)
    } finally {
      teardownKoin()
    }
  }

  // endregion

  // region Track Default Action Tests

  @Test
  fun `displays track default action as Play now`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(trackDefaultAction = TrackAction.PlayNow),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Play Now").performScrollTo().assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `displays track default action as Queue Next`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(trackDefaultAction = TrackAction.QueueNext),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Queue Next").performScrollTo().assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `displays track default action as Queue Last`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(trackDefaultAction = TrackAction.QueueLast),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Queue Last").performScrollTo().assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `displays track default action as Play Now and Queue All`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(trackDefaultAction = TrackAction.PlayNowQueueAll),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Play Now (Queue All)").performScrollTo().assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `track default action click triggers action`() {
    setupKoin()
    try {
      var trackActionClicked = false
      val actions = TestSettingsActions(onTrackDefaultActionClick = { trackActionClicked = true })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Library Track Action").performScrollTo().performClick()
      assertThat(trackActionClicked).isTrue()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `track default action dialog shows all options`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(visibleDialog = SettingsDialogType.TrackDefaultAction),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      // "Play Now" appears in both settings item subtitle and dialog, so check count
      composeTestRule.onAllNodesWithText("Play Now").fetchSemanticsNodes().size.let {
        assertThat(it).isAtLeast(2)
      }
      composeTestRule.onNodeWithText("Queue Next").assertIsDisplayed()
      composeTestRule.onNodeWithText("Queue Last").assertIsDisplayed()
      composeTestRule.onNodeWithText("Play Now (Queue All)").assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `track default action dialog selection triggers onTrackDefaultActionSelected`() {
    setupKoin()
    try {
      var selectedAction: TrackAction? = null
      val actions = TestSettingsActions(onTrackDefaultActionSelected = { selectedAction = it })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(
            trackDefaultAction = TrackAction.PlayNow,
            visibleDialog = SettingsDialogType.TrackDefaultAction
          ),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Queue Next").performClick()
      assertThat(selectedAction).isEqualTo(TrackAction.QueueNext)
    } finally {
      teardownKoin()
    }
  }

  // endregion

  // region Section Headers Tests

  @Test
  fun `displays all section headers`() {
    setupKoin()
    try {
      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(),
          actions = EmptySettingsActions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
      composeTestRule.onNodeWithText("Miscellaneous")
        .performScrollTo()
        .assertIsDisplayed()
      composeTestRule.onNodeWithText("Rating")
        .performScrollTo()
        .assertIsDisplayed()
      composeTestRule.onNodeWithText("Library")
        .performScrollTo()
        .assertIsDisplayed()
      composeTestRule.onNodeWithText("About MusicBee Remote")
        .performScrollTo()
        .assertIsDisplayed()
    } finally {
      teardownKoin()
    }
  }

  // endregion

  // region Dialog Dismiss Tests

  @Test
  fun `dialog dismiss triggers onDismissDialog for theme dialog`() {
    setupKoin()
    try {
      var dismissCalled = false
      val actions = TestSettingsActions(onDismissDialog = { dismissCalled = true })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(visibleDialog = SettingsDialogType.Theme),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      // Click cancel button to dismiss dialog
      composeTestRule.onNodeWithText("Cancel").performClick()
      assertThat(dismissCalled).isTrue()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `dialog dismiss triggers onDismissDialog for incoming call action dialog`() {
    setupKoin()
    try {
      var dismissCalled = false
      val actions = TestSettingsActions(onDismissDialog = { dismissCalled = true })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(visibleDialog = SettingsDialogType.IncomingCallAction),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Cancel").performClick()
      assertThat(dismissCalled).isTrue()
    } finally {
      teardownKoin()
    }
  }

  @Test
  fun `dialog dismiss triggers onDismissDialog for track default action dialog`() {
    setupKoin()
    try {
      var dismissCalled = false
      val actions = TestSettingsActions(onDismissDialog = { dismissCalled = true })

      composeTestRule.setContent {
        SettingsScreenContent(
          state = SettingsContentState(visibleDialog = SettingsDialogType.TrackDefaultAction),
          actions = actions,
          appInfo = testAppInfo
        )
      }

      composeTestRule.onNodeWithText("Cancel").performClick()
      assertThat(dismissCalled).isTrue()
    } finally {
      teardownKoin()
    }
  }

  // endregion
}

/**
 * Test implementation of ISettingsActions that allows tracking which actions were called.
 */
class TestSettingsActions(
  override val onThemeClick: () -> Unit = {},
  override val onThemeSelected: (Theme) -> Unit = {},
  override val onKeepScreenOnChanged: (Boolean) -> Unit = {},
  override val onIncomingCallActionClick: () -> Unit = {},
  override val onIncomingCallActionSelected: (CallAction) -> Unit = {},
  override val onPluginUpdatesChanged: (Boolean) -> Unit = {},
  override val onDebugLoggingChanged: (Boolean) -> Unit = {},
  override val onHalfStarRatingChanged: (Boolean) -> Unit = {},
  override val onShowRatingOnPlayerChanged: (Boolean) -> Unit = {},
  override val onTrackDefaultActionClick: () -> Unit = {},
  override val onTrackDefaultActionSelected: (TrackAction) -> Unit = {},
  override val onNavigateToLicenses: () -> Unit = {},
  override val onNavigateToAppLicense: () -> Unit = {},
  override val onDismissDialog: () -> Unit = {}
) : ISettingsActions
