package com.kelsos.mbrc.screenshots

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.kelsos.mbrc.core.common.settings.TrackAction
import com.kelsos.mbrc.core.ui.theme.RemoteTheme
import com.kelsos.mbrc.feature.settings.compose.EmptySettingsActions
import com.kelsos.mbrc.feature.settings.compose.PreviewAppInfo
import com.kelsos.mbrc.feature.settings.compose.SettingsContentState
import com.kelsos.mbrc.feature.settings.compose.SettingsScreenContent
import com.kelsos.mbrc.feature.settings.data.CallAction
import com.kelsos.mbrc.feature.settings.theme.Theme

private val previewAppInfo = PreviewAppInfo(
  versionName = "1.6.0",
  buildTime = "2025-01-01T12:00:00Z",
  gitRevision = "abc1234"
)

private val previewState = SettingsContentState(
  currentTheme = Theme.System,
  pluginUpdatesEnabled = true,
  debugLoggingEnabled = false,
  incomingCallAction = CallAction.None,
  trackDefaultAction = TrackAction.QueueNext
)

@PreviewTest
@LayoutSizes
@Composable
fun SettingsScreenPreviewLight() {
  RemoteTheme(darkTheme = false) {
    Scaffold { padding ->
      SettingsScreenContent(
        state = previewState,
        actions = EmptySettingsActions,
        appInfo = previewAppInfo
      )
    }
  }
}

@PreviewTest
@Preview(name = "Settings Screen Dark", showBackground = true)
@Composable
fun SettingsScreenPreviewDark() {
  RemoteTheme(darkTheme = true) {
    Scaffold { padding ->
      SettingsScreenContent(
        state = previewState,
        actions = EmptySettingsActions,
        appInfo = previewAppInfo
      )
    }
  }
}
