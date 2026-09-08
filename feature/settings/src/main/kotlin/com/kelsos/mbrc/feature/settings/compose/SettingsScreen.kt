package com.kelsos.mbrc.feature.settings.compose

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelsos.mbrc.core.common.settings.TrackAction
import com.kelsos.mbrc.core.common.utilities.AppInfo
import com.kelsos.mbrc.core.platform.service.ServiceRestarter
import com.kelsos.mbrc.core.ui.compose.ScreenScaffold
import com.kelsos.mbrc.core.ui.layout.ReadableContent
import com.kelsos.mbrc.feature.settings.R
import com.kelsos.mbrc.feature.settings.SettingsDialogType
import com.kelsos.mbrc.feature.settings.SettingsViewModel
import com.kelsos.mbrc.feature.settings.data.CallAction
import com.kelsos.mbrc.feature.settings.theme.Theme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import timber.log.Timber

/**
 * Preview implementation of AppInfo for use in composable previews.
 */
class PreviewAppInfo(
  override val versionName: String = "1.0.0",
  override val versionCode: Int = 1,
  override val buildTime: String = "2025-01-01T00:00:00Z",
  override val gitRevision: String = "preview",
  override val applicationId: String = "com.kelsos.mbrc.preview"
) : AppInfo

/**
 * Immutable state for settings content display.
 * Used for both the actual screen and screenshot tests.
 */
@Immutable
data class SettingsContentState(
  val currentTheme: Theme = Theme.System,
  val keepScreenOnEnabled: Boolean = false,
  val pluginUpdatesEnabled: Boolean = false,
  val debugLoggingEnabled: Boolean = false,
  val incomingCallAction: CallAction = CallAction.None,
  val trackDefaultAction: TrackAction = TrackAction.PlayNow,
  val halfStarRatingEnabled: Boolean = true,
  val showRatingOnPlayerEnabled: Boolean = false,
  val visibleDialog: SettingsDialogType? = null
)

/**
 * Stable interface for settings actions to avoid recomposition.
 */
@Stable
interface ISettingsActions {
  val onThemeClick: () -> Unit
  val onThemeSelected: (Theme) -> Unit
  val onKeepScreenOnChanged: (Boolean) -> Unit
  val onIncomingCallActionClick: () -> Unit
  val onIncomingCallActionSelected: (CallAction) -> Unit
  val onPluginUpdatesChanged: (Boolean) -> Unit
  val onDebugLoggingChanged: (Boolean) -> Unit
  val onHalfStarRatingChanged: (Boolean) -> Unit
  val onShowRatingOnPlayerChanged: (Boolean) -> Unit
  val onTrackDefaultActionClick: () -> Unit
  val onTrackDefaultActionSelected: (TrackAction) -> Unit
  val onNavigateToLicenses: () -> Unit
  val onNavigateToAppLicense: () -> Unit
  val onDismissDialog: () -> Unit
}

/**
 * Empty actions for preview/testing.
 */
object EmptySettingsActions : ISettingsActions {
  override val onThemeClick: () -> Unit = {}
  override val onThemeSelected: (Theme) -> Unit = {}
  override val onKeepScreenOnChanged: (Boolean) -> Unit = {}
  override val onIncomingCallActionClick: () -> Unit = {}
  override val onIncomingCallActionSelected: (CallAction) -> Unit = {}
  override val onPluginUpdatesChanged: (Boolean) -> Unit = {}
  override val onDebugLoggingChanged: (Boolean) -> Unit = {}
  override val onHalfStarRatingChanged: (Boolean) -> Unit = {}
  override val onShowRatingOnPlayerChanged: (Boolean) -> Unit = {}
  override val onTrackDefaultActionClick: () -> Unit = {}
  override val onTrackDefaultActionSelected: (TrackAction) -> Unit = {}
  override val onNavigateToLicenses: () -> Unit = {}
  override val onNavigateToAppLicense: () -> Unit = {}
  override val onDismissDialog: () -> Unit = {}
}

/**
 * Appearance settings section.
 */
@Composable
private fun AppearanceSettingsSection(viewModel: SettingsViewModel) {
  val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
  val visibleDialog by viewModel.visibleDialog.collectAsStateWithLifecycle()
  val keepScreenOnEnabled by viewModel.keepScreenOnEnabled.collectAsStateWithLifecycle()

  SettingsSection(title = stringResource(R.string.settings_appearance)) {
    SettingsItem(
      title = stringResource(R.string.setting_appearance_theme),
      subtitle = getThemeDisplayName(currentTheme),
      onClick = { viewModel.showDialog(SettingsDialogType.Theme) }
    )

    SettingsToggleItem(
      title = stringResource(R.string.setting_keep_screen_on),
      subtitle = stringResource(R.string.setting_keep_screen_on_summary),
      checked = keepScreenOnEnabled,
      onCheckedChange = { enabled -> viewModel.updateKeepScreenOn(enabled) }
    )
  }

  // Theme Selection Dialog
  if (visibleDialog == SettingsDialogType.Theme) {
    ThemeSelectionDialog(
      currentTheme = currentTheme,
      onThemeSelected = { theme ->
        viewModel.updateTheme(theme)
        viewModel.hideDialog()
      },
      onDismiss = { viewModel.hideDialog() }
    )
  }
}

/**
 * Miscellaneous settings section.
 */
@Composable
private fun MiscellaneousSettingsSection(
  viewModel: SettingsViewModel,
  context: Context,
  phonePermissionLauncher: ActivityResultLauncher<String>,
  serviceRestarter: ServiceRestarter
) {
  // Reactive state management
  val pluginUpdatesEnabled by viewModel.pluginUpdatesEnabled.collectAsStateWithLifecycle()
  val debugLoggingEnabled by viewModel.debugLoggingEnabled.collectAsStateWithLifecycle()
  val incomingCallAction by viewModel.incomingCallAction.collectAsStateWithLifecycle()
  val visibleDialog by viewModel.visibleDialog.collectAsStateWithLifecycle()

  SettingsSection(title = stringResource(R.string.settings_miscellaneous)) {
    SettingsItem(
      title = stringResource(R.string.settings_miscellaneous_incoming_call_action),
      subtitle = getIncomingCallActionDisplayName(incomingCallAction),
      onClick = {
        if (hasPhonePermission(context)) {
          viewModel.showDialog(SettingsDialogType.IncomingCallAction)
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
        viewModel.updatePluginUpdates(enabled)
      }
    )

    SettingsToggleItem(
      title = stringResource(R.string.setting_miscellaneous_debug_logging),
      subtitle = stringResource(R.string.setting_miscellaneous_debug_logging_summary),
      checked = debugLoggingEnabled,
      onCheckedChange = { enabled ->
        viewModel.updateDebugLogging(enabled)
      }
    )
  }

  // Incoming Call Action Dialog
  if (visibleDialog == SettingsDialogType.IncomingCallAction) {
    IncomingCallActionDialog(
      currentAction = incomingCallAction,
      onActionSelected = { action ->
        viewModel.updateIncomingCallAction(action)
        viewModel.hideDialog()
      },
      onDismiss = { viewModel.hideDialog() }
    )
  }
}

/**
 * Rating settings section.
 */
@Composable
private fun RatingSettingsSection(viewModel: SettingsViewModel) {
  val halfStarRatingEnabled by viewModel.halfStarRatingEnabled.collectAsStateWithLifecycle()
  val showRatingOnPlayerEnabled by viewModel.showRatingOnPlayerEnabled.collectAsStateWithLifecycle()

  SettingsSection(title = stringResource(R.string.settings_rating)) {
    SettingsToggleItem(
      title = stringResource(R.string.setting_half_star_rating),
      subtitle = stringResource(R.string.setting_half_star_rating_summary),
      checked = halfStarRatingEnabled,
      onCheckedChange = { enabled ->
        viewModel.updateHalfStarRating(enabled)
      }
    )

    SettingsToggleItem(
      title = stringResource(R.string.setting_show_rating_on_player),
      subtitle = stringResource(R.string.setting_show_rating_on_player_summary),
      checked = showRatingOnPlayerEnabled,
      onCheckedChange = { enabled ->
        viewModel.updateShowRatingOnPlayer(enabled)
      }
    )
  }
}

/**
 * Library settings section.
 */
@Composable
private fun LibrarySettingsSection(viewModel: SettingsViewModel) {
  val trackDefaultAction by viewModel.trackDefaultAction.collectAsStateWithLifecycle()
  val visibleDialog by viewModel.visibleDialog.collectAsStateWithLifecycle()

  SettingsSection(title = stringResource(R.string.common_library)) {
    SettingsItem(
      title = stringResource(R.string.preferences_library_track_default_action_title),
      subtitle = getTrackActionDisplayName(trackDefaultAction),
      onClick = { viewModel.showDialog(SettingsDialogType.TrackDefaultAction) }
    )
  }

  // Track Default Action Dialog
  if (visibleDialog == SettingsDialogType.TrackDefaultAction) {
    TrackDefaultActionDialog(
      currentAction = trackDefaultAction,
      onActionSelected = { action ->
        viewModel.updateTrackDefaultAction(action)
        viewModel.hideDialog()
      },
      onDismiss = { viewModel.hideDialog() }
    )
  }
}

/**
 * About settings section.
 */
@Composable
private fun AboutSettingsSection(
  onNavigateToLicenses: () -> Unit,
  onNavigateToAppLicense: () -> Unit,
  appInfo: AppInfo = koinInject()
) {
  SettingsSection(title = stringResource(R.string.preferences_category_about)) {
    SettingsItem(
      title = stringResource(R.string.settings_oss_license),
      onClick = onNavigateToLicenses
    )

    SettingsItem(
      title = stringResource(R.string.settings_title_license),
      onClick = onNavigateToAppLicense
    )

    SettingsItem(
      title = stringResource(R.string.preferences_about_version),
      subtitle = stringResource(
        R.string.settings_version_number,
        appInfo.versionName
      )
    )

    SettingsItem(
      title = stringResource(R.string.settings_build_time_title),
      subtitle = appInfo.buildTime
    )

    SettingsItem(
      title = stringResource(R.string.settings_revision_title),
      subtitle = appInfo.gitRevision
    )
  }
}

/**
 * Settings screen implemented in Jetpack Compose.
 * Provides all application settings and preferences.
 */
@Composable
fun SettingsScreen(
  snackbarHostState: SnackbarHostState,
  onOpenDrawer: () -> Unit,
  modifier: Modifier = Modifier,
  onNavigateToLicenses: () -> Unit = {},
  onNavigateToAppLicense: () -> Unit = {},
  viewModel: SettingsViewModel = koinViewModel()
) {
  val context = LocalContext.current
  val serviceRestarter: ServiceRestarter = koinInject()
  val title = stringResource(R.string.common_settings)

  // Permission launcher for phone state
  val phonePermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      Timber.v("Permission granted for READ_PHONE_STATE")
      serviceRestarter.restartService()
    } else {
      Timber.w("Permission denied for READ_PHONE_STATE")
    }
  }

  ScreenScaffold(
    title = title,
    snackbarHostState = snackbarHostState,
    onOpenDrawer = onOpenDrawer,
    modifier = modifier
  ) { paddingValues ->
    SettingsContent(
      viewModel = viewModel,
      context = context,
      phonePermissionLauncher = phonePermissionLauncher,
      serviceRestarter = serviceRestarter,
      onNavigateToLicenses = onNavigateToLicenses,
      onNavigateToAppLicense = onNavigateToAppLicense,
      paddingValues = paddingValues
    )
  }
}

@Composable
private fun SettingsContent(
  viewModel: SettingsViewModel,
  context: Context,
  phonePermissionLauncher: ActivityResultLauncher<String>,
  serviceRestarter: ServiceRestarter,
  onNavigateToLicenses: () -> Unit,
  onNavigateToAppLicense: () -> Unit,
  paddingValues: PaddingValues
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(paddingValues)
      .verticalScroll(rememberScrollState())
      .padding(vertical = 8.dp)
  ) {
    // Appearance Settings Section
    AppearanceSettingsSection(viewModel = viewModel)

    SettingsDivider()

    // Miscellaneous Settings Section
    MiscellaneousSettingsSection(
      viewModel = viewModel,
      context = context,
      phonePermissionLauncher = phonePermissionLauncher,
      serviceRestarter = serviceRestarter
    )

    SettingsDivider()

    // Rating Settings Section
    RatingSettingsSection(viewModel = viewModel)

    SettingsDivider()

    // Library Settings Section
    LibrarySettingsSection(viewModel = viewModel)

    SettingsDivider()

    // About Section
    AboutSettingsSection(
      onNavigateToLicenses = onNavigateToLicenses,
      onNavigateToAppLicense = onNavigateToAppLicense
    )
  }
}

/**
 * Settings screen content that can be used for both the actual screen and screenshot tests.
 * Takes immutable state and stable actions to avoid unnecessary recomposition.
 */
@Composable
fun SettingsScreenContent(
  state: SettingsContentState,
  actions: ISettingsActions,
  modifier: Modifier = Modifier,
  appInfo: AppInfo = koinInject()
) {
  ReadableContent(modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(vertical = 8.dp)
    ) {
      // Appearance Settings Section
      AppearanceContentSection(
        currentTheme = state.currentTheme,
        keepScreenOnEnabled = state.keepScreenOnEnabled,
        onThemeClick = actions.onThemeClick,
        onKeepScreenOnChanged = actions.onKeepScreenOnChanged
      )

      SettingsDivider()

      // Miscellaneous Settings Section
      MiscellaneousContentSection(
        incomingCallAction = state.incomingCallAction,
        pluginUpdatesEnabled = state.pluginUpdatesEnabled,
        debugLoggingEnabled = state.debugLoggingEnabled,
        onIncomingCallActionClick = actions.onIncomingCallActionClick,
        onPluginUpdatesChanged = actions.onPluginUpdatesChanged,
        onDebugLoggingChanged = actions.onDebugLoggingChanged
      )

      SettingsDivider()

      // Rating Settings Section
      RatingContentSection(
        halfStarRatingEnabled = state.halfStarRatingEnabled,
        showRatingOnPlayerEnabled = state.showRatingOnPlayerEnabled,
        onHalfStarRatingChanged = actions.onHalfStarRatingChanged,
        onShowRatingOnPlayerChanged = actions.onShowRatingOnPlayerChanged
      )

      SettingsDivider()

      // Library Settings Section
      LibraryContentSection(
        trackDefaultAction = state.trackDefaultAction,
        onTrackDefaultActionClick = actions.onTrackDefaultActionClick
      )

      SettingsDivider()

      // About Section
      AboutContentSection(
        appInfo = appInfo,
        onNavigateToLicenses = actions.onNavigateToLicenses,
        onNavigateToAppLicense = actions.onNavigateToAppLicense
      )
    }
  }

  // Dialogs
  when (state.visibleDialog) {
    SettingsDialogType.Theme -> ThemeSelectionDialog(
      currentTheme = state.currentTheme,
      onThemeSelected = { theme ->
        actions.onThemeSelected(theme)
        actions.onDismissDialog()
      },
      onDismiss = actions.onDismissDialog
    )

    SettingsDialogType.IncomingCallAction -> IncomingCallActionDialog(
      currentAction = state.incomingCallAction,
      onActionSelected = { action ->
        actions.onIncomingCallActionSelected(action)
        actions.onDismissDialog()
      },
      onDismiss = actions.onDismissDialog
    )

    SettingsDialogType.TrackDefaultAction -> TrackDefaultActionDialog(
      currentAction = state.trackDefaultAction,
      onActionSelected = { action ->
        actions.onTrackDefaultActionSelected(action)
        actions.onDismissDialog()
      },
      onDismiss = actions.onDismissDialog
    )

    null -> { /* No dialog */ }
  }
}

/**
 * Appearance section for content composable.
 */
@Composable
private fun AppearanceContentSection(
  currentTheme: Theme,
  keepScreenOnEnabled: Boolean,
  onThemeClick: () -> Unit,
  onKeepScreenOnChanged: (Boolean) -> Unit
) {
  SettingsSection(title = stringResource(R.string.settings_appearance)) {
    SettingsItem(
      title = stringResource(R.string.setting_appearance_theme),
      subtitle = getThemeDisplayName(currentTheme),
      onClick = onThemeClick
    )

    SettingsToggleItem(
      title = stringResource(R.string.setting_keep_screen_on),
      subtitle = stringResource(R.string.setting_keep_screen_on_summary),
      checked = keepScreenOnEnabled,
      onCheckedChange = onKeepScreenOnChanged
    )
  }
}

/**
 * Miscellaneous section for content composable.
 */
@Composable
private fun MiscellaneousContentSection(
  incomingCallAction: CallAction,
  pluginUpdatesEnabled: Boolean,
  debugLoggingEnabled: Boolean,
  onIncomingCallActionClick: () -> Unit,
  onPluginUpdatesChanged: (Boolean) -> Unit,
  onDebugLoggingChanged: (Boolean) -> Unit
) {
  SettingsSection(title = stringResource(R.string.settings_miscellaneous)) {
    SettingsItem(
      title = stringResource(R.string.settings_miscellaneous_incoming_call_action),
      subtitle = getIncomingCallActionDisplayName(incomingCallAction),
      onClick = onIncomingCallActionClick
    )

    SettingsToggleItem(
      title = stringResource(R.string.setting_miscellaneous_plugin_updates),
      subtitle = stringResource(R.string.setting_miscellaneous_plugin_updates_summary),
      checked = pluginUpdatesEnabled,
      onCheckedChange = onPluginUpdatesChanged
    )

    SettingsToggleItem(
      title = stringResource(R.string.setting_miscellaneous_debug_logging),
      subtitle = stringResource(R.string.setting_miscellaneous_debug_logging_summary),
      checked = debugLoggingEnabled,
      onCheckedChange = onDebugLoggingChanged
    )
  }
}

/**
 * Rating section for content composable.
 */
@Composable
private fun RatingContentSection(
  halfStarRatingEnabled: Boolean,
  showRatingOnPlayerEnabled: Boolean,
  onHalfStarRatingChanged: (Boolean) -> Unit,
  onShowRatingOnPlayerChanged: (Boolean) -> Unit
) {
  SettingsSection(title = stringResource(R.string.settings_rating)) {
    SettingsToggleItem(
      title = stringResource(R.string.setting_half_star_rating),
      subtitle = stringResource(R.string.setting_half_star_rating_summary),
      checked = halfStarRatingEnabled,
      onCheckedChange = onHalfStarRatingChanged
    )

    SettingsToggleItem(
      title = stringResource(R.string.setting_show_rating_on_player),
      subtitle = stringResource(R.string.setting_show_rating_on_player_summary),
      checked = showRatingOnPlayerEnabled,
      onCheckedChange = onShowRatingOnPlayerChanged
    )
  }
}

/**
 * Library section for content composable.
 */
@Composable
private fun LibraryContentSection(
  trackDefaultAction: TrackAction,
  onTrackDefaultActionClick: () -> Unit
) {
  SettingsSection(title = stringResource(R.string.common_library)) {
    SettingsItem(
      title = stringResource(R.string.preferences_library_track_default_action_title),
      subtitle = getTrackActionDisplayName(trackDefaultAction),
      onClick = onTrackDefaultActionClick
    )
  }
}

/**
 * About section for content composable.
 */
@Composable
private fun AboutContentSection(
  appInfo: AppInfo,
  onNavigateToLicenses: () -> Unit,
  onNavigateToAppLicense: () -> Unit
) {
  SettingsSection(title = stringResource(R.string.preferences_category_about)) {
    SettingsItem(
      title = stringResource(R.string.settings_oss_license),
      onClick = onNavigateToLicenses
    )

    SettingsItem(
      title = stringResource(R.string.settings_title_license),
      onClick = onNavigateToAppLicense
    )

    SettingsItem(
      title = stringResource(R.string.preferences_about_version),
      subtitle = stringResource(R.string.settings_version_number, appInfo.versionName)
    )

    SettingsItem(
      title = stringResource(R.string.settings_build_time_title),
      subtitle = appInfo.buildTime
    )

    SettingsItem(
      title = stringResource(R.string.settings_revision_title),
      subtitle = appInfo.gitRevision
    )
  }
}

/**
 * Divider between settings sections.
 */
@Composable
internal fun SettingsDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(vertical = 8.dp),
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
  )
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
 * Settings section header with title.
 */
@Composable
internal fun SettingsSection(title: String, content: @Composable () -> Unit) {
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
internal fun SettingsItem(title: String, subtitle: String? = null, onClick: () -> Unit = {}) {
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
internal fun SettingsToggleItem(
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
