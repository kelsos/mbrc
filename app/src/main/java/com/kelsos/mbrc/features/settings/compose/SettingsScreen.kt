package com.kelsos.mbrc.features.settings.compose

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import com.kelsos.mbrc.BuildConfig
import com.kelsos.mbrc.R
import com.kelsos.mbrc.app.ConfigurableScreen
import com.kelsos.mbrc.app.ScreenConfig
import com.kelsos.mbrc.common.utilities.RemoteUtils
import com.kelsos.mbrc.features.settings.CallAction
import com.kelsos.mbrc.features.settings.TrackAction
import com.kelsos.mbrc.features.theme.Theme
import com.kelsos.mbrc.platform.RemoteService
import timber.log.Timber

private const val SERVICE_RESTART_DELAY = 500L

private const val WEBVIEW_TEXT_ZOOM = 120
private const val WEBVIEW_DEFAULT_FONT_SIZE = 16
private const val WEBVIEW_MINIMUM_FONT_SIZE = 14

/**
 * Settings screen configuration implementing ConfigurableScreen.
 * Returns empty configuration as Settings screen doesn't need special scaffold elements.
 */
class SettingsScreenConfig : ConfigurableScreen {
  @Composable
  override fun getScreenConfig(): ScreenConfig = ScreenConfig.Empty
}

/**
 * Appearance settings section.
 */
@Composable
private fun AppearanceSettingsSection(state: SettingsScreenState) {
  val currentTheme by state.currentTheme()

  SettingsSection(title = stringResource(R.string.settings_appearance)) {
    SettingsItem(
      title = stringResource(R.string.setting_appearance_theme),
      subtitle = getThemeDisplayName(currentTheme),
      onClick = { state.showDialog(DialogType.Theme) }
    )
  }

  // Theme Selection Dialog
  if (state.visibleDialog == DialogType.Theme) {
    ThemeSelectionDialog(
      currentTheme = currentTheme,
      onThemeSelected = { theme ->
        state.updateTheme(theme)
        state.hideDialog()
      },
      onDismiss = { state.hideDialog() }
    )
  }
}

/**
 * Miscellaneous settings section.
 */
@Composable
private fun MiscellaneousSettingsSection(
  state: SettingsScreenState,
  context: Context,
  phonePermissionLauncher: ActivityResultLauncher<String>
) {
  // Reactive state management
  val pluginUpdatesEnabled by state.pluginUpdatesEnabled()
  val debugLoggingEnabled by state.debugLoggingEnabled()
  val incomingCallAction by state.incomingCallAction()

  SettingsSection(title = stringResource(R.string.settings_miscellaneous)) {
    SettingsItem(
      title = stringResource(R.string.settings_miscellaneous_incoming_call_action),
      subtitle = getIncomingCallActionDisplayName(incomingCallAction),
      onClick = {
        if (hasPhonePermission(context)) {
          state.showDialog(DialogType.IncomingCallAction)
        } else {
          phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
      }
    )

    SettingsToggleItem(
      title = stringResource(R.string.setting_miscellaneous_plugin_updates),
      subtitle = stringResource(R.string.setting_miscellaneous_plugin_updates_summary),
      checked = pluginUpdatesEnabled,
      onCheckedChange = { enabled ->
        state.updatePluginUpdates(enabled)
      }
    )

    SettingsToggleItem(
      title = stringResource(R.string.setting_miscellaneous_debug_logging),
      subtitle = stringResource(R.string.setting_miscellaneous_debug_logging_summary),
      checked = debugLoggingEnabled,
      onCheckedChange = { enabled ->
        state.updateDebugLogging(enabled)
      }
    )
  }

  // Incoming Call Action Dialog
  if (state.visibleDialog == DialogType.IncomingCallAction) {
    IncomingCallActionDialog(
      currentAction = incomingCallAction,
      onActionSelected = { action ->
        state.updateIncomingCallAction(action)
        state.hideDialog()
      },
      onDismiss = { state.hideDialog() }
    )
  }
}

/**
 * Library settings section.
 */
@Composable
private fun LibrarySettingsSection(state: SettingsScreenState) {
  val trackDefaultAction by state.trackDefaultAction()

  SettingsItem(
    title = stringResource(R.string.preferences_library_track_default_action_title),
    subtitle = getTrackActionDisplayName(trackDefaultAction),
    onClick = { state.showDialog(DialogType.TrackDefaultAction) }
  )

  // Track Default Action Dialog
  if (state.visibleDialog == DialogType.TrackDefaultAction) {
    TrackDefaultActionDialog(
      currentAction = trackDefaultAction,
      onActionSelected = { action ->
        state.updateTrackDefaultAction(action)
        state.hideDialog()
      },
      onDismiss = { state.hideDialog() }
    )
  }
}

/**
 * About settings section.
 */
@Composable
private fun AboutSettingsSection() {
  val context = LocalContext.current
  val licenseDialogState = rememberLicenseDialogState()

  SettingsSection(title = stringResource(R.string.preferences_category_about)) {
    SettingsItem(
      title = stringResource(R.string.settings_oss_license),
      onClick = {
        licenseDialogState.showLicense(
          "file:///android_asset/licenses.html",
          context.getString(R.string.open_source_licenses_title)
        )
      }
    )

    SettingsItem(
      title = stringResource(R.string.settings_title_license),
      onClick = {
        licenseDialogState.showLicense(
          "file:///android_asset/license.html",
          context.getString(R.string.musicbee_remote_license_title)
        )
      }
    )

    SettingsItem(
      title = stringResource(R.string.preferences_about_version),
      subtitle = stringResource(
        R.string.settings_version_number,
        RemoteUtils.VERSION
      )
    )

    SettingsItem(
      title = stringResource(R.string.settings_build_time_title),
      subtitle = BuildConfig.BUILD_TIME
    )

    SettingsItem(
      title = stringResource(R.string.settings_revision_title),
      subtitle = BuildConfig.GIT_SHA
    )
  }

  // License WebView Dialog
  if (licenseDialogState.isVisible) {
    LicenseWebViewDialog(
      url = licenseDialogState.url,
      title = licenseDialogState.title,
      onDismiss = licenseDialogState::hideLicense
    )
  }
}

/**
 * Settings screen implemented in Jetpack Compose.
 * Provides all application settings and preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val state = rememberSettingsScreenState()

  // Permission launcher for phone state
  val phonePermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      Timber.v("Permission granted for READ_PHONE_STATE")
      restartService(context)
    } else {
      Timber.w("Permission denied for READ_PHONE_STATE")
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    // Appearance Settings Section
    AppearanceSettingsSection(state = state)

    // Miscellaneous Settings Section
    MiscellaneousSettingsSection(
      state = state,
      context = context,
      phonePermissionLauncher = phonePermissionLauncher
    )

    // Library Settings Section
    LibrarySettingsSection(state = state)

    // About Section
    AboutSettingsSection()
  }
}

/**
 * Creates a ConfigurableScreen instance for the SettingsScreen.
 */
@Composable
fun rememberSettingsScreenConfig(): ConfigurableScreen = remember {
  SettingsScreenConfig()
}

/**
 * Generic radio selection dialog for choosing from a list of options.
 */
@Composable
private fun RadioSelectionDialog(
  title: String,
  values: Array<String>,
  labels: Array<String>,
  currentValue: String,
  onValueSelected: (String) -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(modifier = Modifier.selectableGroup()) {
        values.forEachIndexed { index, value ->
          Row(
            Modifier
              .fillMaxWidth()
              .height(48.dp)
              .selectable(
                selected = value == currentValue,
                onClick = { onValueSelected(value) },
                role = Role.RadioButton
              )
              .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = value == currentValue,
              onClick = null
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(labels[index])
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(android.R.string.cancel))
      }
    }
  )
}

/**
 * Theme selection dialog for choosing app theme.
 */
@Composable
private fun ThemeSelectionDialog(
  currentTheme: Theme,
  onThemeSelected: (Theme) -> Unit,
  onDismiss: () -> Unit
) {
  RadioSelectionDialog(
    title = stringResource(R.string.setting_appearance_theme),
    values = stringArrayResource(R.array.theme_options_values),
    labels = stringArrayResource(R.array.theme_options),
    currentValue = currentTheme.value,
    onValueSelected = { themeString -> onThemeSelected(Theme.fromString(themeString)) },
    onDismiss = onDismiss
  )
}

/**
 * Incoming call action selection dialog.
 */
@Composable
private fun IncomingCallActionDialog(
  currentAction: CallAction,
  onActionSelected: (CallAction) -> Unit,
  onDismiss: () -> Unit
) {
  RadioSelectionDialog(
    title = stringResource(R.string.settings_miscellaneous_incoming_call_action),
    values = stringArrayResource(R.array.incoming_action_options_values),
    labels = stringArrayResource(R.array.incoming_action_options),
    currentValue = currentAction.string,
    onValueSelected = { actionString -> onActionSelected(CallAction.fromString(actionString)) },
    onDismiss = onDismiss
  )
}

/**
 * Track default action selection dialog.
 */
@Composable
private fun TrackDefaultActionDialog(
  currentAction: TrackAction,
  onActionSelected: (TrackAction) -> Unit,
  onDismiss: () -> Unit
) {
  RadioSelectionDialog(
    title = stringResource(R.string.preferences_library_track_default_action_title),
    values = stringArrayResource(R.array.preferences_library_track_default_action_option_values),
    labels = stringArrayResource(R.array.preferences_library_track_default_action_option_labels),
    currentValue = currentAction.value,
    onValueSelected = { actionString -> onActionSelected(TrackAction.fromString(actionString)) },
    onDismiss = onDismiss
  )
}

/**
 * WebView dialog for displaying license content.
 */
@Composable
private fun LicenseWebViewDialog(url: String, title: String, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      AndroidView(
        factory = { context ->
          WebView(context).apply {
            webViewClient = WebViewClient()
            settings.apply {
              javaScriptEnabled = false
              loadWithOverviewMode = true
              useWideViewPort = true
              setSupportZoom(true)
              builtInZoomControls = true
              displayZoomControls = false
              textZoom = WEBVIEW_TEXT_ZOOM
              defaultFontSize = WEBVIEW_DEFAULT_FONT_SIZE
              minimumFontSize = WEBVIEW_MINIMUM_FONT_SIZE
            }
          }
        },
        update = { webView ->
          webView.loadUrl(url)
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(400.dp)
      )
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(android.R.string.ok))
      }
    }
  )
}

/**
 * Settings section header with title.
 */
@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    content()
  }
}

/**
 * Individual settings item.
 */
@Composable
private fun SettingsItem(title: String, subtitle: String? = null, onClick: () -> Unit = {}) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium
      )
      if (subtitle != null) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

/**
 * Settings item with a toggle switch.
 */
@Composable
private fun SettingsToggleItem(
  title: String,
  subtitle: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onCheckedChange(!checked) }
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium
      )
      if (subtitle != null) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
    Spacer(modifier = Modifier.width(16.dp))
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange
    )
  }
}

// Helper functions
@Composable
private fun getThemeDisplayName(theme: Theme): String = when (theme) {
  Theme.System -> stringResource(R.string.theme_system)
  Theme.Light -> stringResource(R.string.theme_light)
  Theme.Dark -> stringResource(R.string.theme_dark)
}

@Composable
private fun getIncomingCallActionDisplayName(action: CallAction): String = when (action) {
  CallAction.None -> stringResource(R.string.action_none)
  CallAction.Pause -> stringResource(R.string.action_pause)
  CallAction.Reduce -> stringResource(R.string.action_reduce_volume)
  CallAction.Stop -> stringResource(R.string.action_stop)
}

@Composable
private fun getTrackActionDisplayName(trackAction: TrackAction): String = when (trackAction) {
  TrackAction.QueueNext -> stringResource(R.string.menu_queue_next)
  TrackAction.QueueLast -> stringResource(R.string.menu_queue_last)
  TrackAction.PlayNow -> stringResource(R.string.menu_play)
  TrackAction.PlayNowQueueAll -> stringResource(R.string.menu_play_queue_all)
}

private fun hasPhonePermission(context: Context): Boolean = ContextCompat.checkSelfPermission(
  context,
  Manifest.permission.READ_PHONE_STATE
) == PackageManager.PERMISSION_GRANTED

private fun restartService(context: Context) {
  Timber.v("Restarting service")
  val intent = Intent(context, RemoteService::class.java)
  context.stopService(intent)
  val handler = Handler(Looper.getMainLooper())
  HandlerCompat.postDelayed(handler, {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(Intent(context, RemoteService::class.java))
    } else {
      context.startService(Intent(context, RemoteService::class.java))
    }
  }, null, SERVICE_RESTART_DELAY)
}
