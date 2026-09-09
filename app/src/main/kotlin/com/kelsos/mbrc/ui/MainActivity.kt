package com.kelsos.mbrc.ui

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kelsos.mbrc.core.common.state.ConnectionStatePublisher
import com.kelsos.mbrc.core.common.state.ConnectionStatus
import com.kelsos.mbrc.core.networking.LocalNetworkAccess
import com.kelsos.mbrc.feature.settings.data.KeepScreenOn
import com.kelsos.mbrc.feature.settings.domain.SettingsManager
import com.kelsos.mbrc.service.LocalNetworkAccessImpl
import com.kelsos.mbrc.service.ServiceChecker
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Main entry point for the Compose-based MusicBee Remote app.
 * This replaces the traditional fragment-based navigation with Compose Navigation.
 */
class MainActivity : ComponentActivity() {

  private val serviceChecker: ServiceChecker by inject()
  private val localNetworkAccess: LocalNetworkAccess by inject()
  private val connectionState: ConnectionStatePublisher by inject()
  private val preferences: SharedPreferences by inject()
  private val settingsManager: SettingsManager by inject()

  private var showLocalNetworkRationale by mutableStateOf(false)

  /**
   * Whether the user has already turned the rationale down. Kept across configuration changes so a
   * rotation does not put the dialog back in their face after they dismissed it.
   */
  private var rationaleDeclined = false

  /**
   * Whether the system prompt has ever been launched.
   *
   * `shouldShowRequestPermissionRationale` cannot answer this on its own: it is false both before
   * the first request and after a permanent denial. Persisted rather than kept in the instance
   * state because the distinction has to survive the process being killed, otherwise a cold start
   * looks like "never asked" and Grant calls a prompt the system will not show.
   */
  private var permissionRequested: Boolean
    get() = preferences.getBoolean(PREF_PERMISSION_REQUESTED, false)
    set(value) = preferences.edit { putBoolean(PREF_PERMISSION_REQUESTED, value) }

  private val localNetworkPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        serviceChecker.startServiceIfNotRunning()
        return@registerForActivityResult
      }
      denyLocalNetwork()
      // A permanently denied permission makes launch() return instantly without showing anything,
      // so without this the Grant action would appear to do nothing at all.
      if (!shouldShowRequestPermissionRationale(ACCESS_LOCAL_NETWORK)) {
        openAppSettings()
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    // Install the splash screen before calling super.onCreate()
    installSplashScreen()

    super.onCreate(savedInstanceState)

    rationaleDeclined = savedInstanceState?.getBoolean(STATE_RATIONALE_DECLINED) == true

    // Enable edge-to-edge display with transparent system bars
    // Using auto() with transparent scrims for both light and dark themes
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(
        lightScrim = Color.TRANSPARENT,
        darkScrim = Color.TRANSPARENT
      ),
      navigationBarStyle = SystemBarStyle.auto(
        lightScrim = Color.TRANSPARENT,
        darkScrim = Color.TRANSPARENT
      )
    )

    // Allow content to draw behind system bars
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // Start the remote service if not already running (same as BaseActivity)
    ensureLocalNetworkAccessThenStart(restored = savedInstanceState != null)

    keepScreenOnWhileVisible()

    setContent {
      RemoteApp(
        onRequestLocalNetworkAccess = ::requestLocalNetworkAccess,
        showLocalNetworkRationale = showLocalNetworkRationale,
        onLocalNetworkRationaleContinue = {
          showLocalNetworkRationale = false
          permissionRequested = true
          localNetworkPermissionLauncher.launch(ACCESS_LOCAL_NETWORK)
        },
        onLocalNetworkRationaleDismiss = {
          showLocalNetworkRationale = false
          rationaleDeclined = true
          denyLocalNetwork()
        }
      )
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(STATE_RATIONALE_DECLINED, rationaleDeclined)
  }

  /**
   * Holds the screen awake according to the setting, for as long as the activity is visible.
   *
   * Collected inside [repeatOnLifecycle] at STARTED so the flag is cleared on the way to the
   * background: a remote is useful propped up on a desk, but not at the cost of a device that
   * never sleeps once the user has moved on. The window flag is inert for a window that is not
   * in front anyway, so this is belt and braces rather than the only guard.
   *
   * [KeepScreenOn.WhileCharging] is the tier that answers the battery objection: propped on a
   * counter almost always means plugged in, and that is when a display timeout is pure nuisance.
   */
  private fun keepScreenOnWhileVisible() {
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        try {
          combine(
            settingsManager.keepScreenOnFlow,
            chargingState()
          ) { mode, charging ->
            when (mode) {
              KeepScreenOn.Never -> false
              KeepScreenOn.WhileCharging -> charging
              KeepScreenOn.Always -> true
            }
          }.distinctUntilChanged().collect { keepAwake ->
            if (keepAwake) {
              window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
              window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
          }
        } finally {
          window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
      }
    }
  }

  /**
   * On Android 17 (API 37) the local network is gated behind the `ACCESS_LOCAL_NETWORK` runtime
   * permission; without it the TCP/multicast connection to MusicBee is blocked. Explain that before
   * the system prompt appears, since the system dialog carries no context of its own. Older
   * releases and the already-granted case start the service directly.
   *
   * @param restored true when the activity is being recreated, in which case the dialog state comes
   * from the saved instance state rather than being decided again.
   */
  private fun ensureLocalNetworkAccessThenStart(restored: Boolean) {
    if (localNetworkAccess.isPermitted()) {
      serviceChecker.startServiceIfNotRunning()
      return
    }
    if (!restored && !rationaleDeclined) {
      showLocalNetworkRationale = true
      return
    }
    denyLocalNetwork()
  }

  /**
   * Records that nothing can connect. The service is deliberately not started: it exists to hold a
   * connection, so without access it would only leave a foreground notification for a connection
   * that can never happen.
   */
  private fun denyLocalNetwork() {
    connectionState.updateConnection(ConnectionStatus.LocalNetworkDenied)
  }

  /**
   * Sends the user somewhere they can actually grant access: the prompt while the system still
   * shows it, the app's settings page once it does not.
   */
  private fun requestLocalNetworkAccess() {
    if (localNetworkAccess.isPermitted()) {
      return
    }
    val promptAvailable = !permissionRequested ||
      shouldShowRequestPermissionRationale(ACCESS_LOCAL_NETWORK)
    if (promptAvailable) {
      permissionRequested = true
      localNetworkPermissionLauncher.launch(ACCESS_LOCAL_NETWORK)
      return
    }
    openAppSettings()
  }

  private fun openAppSettings() {
    startActivity(
      Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
      )
    )
  }

  private companion object {
    const val ACCESS_LOCAL_NETWORK = LocalNetworkAccessImpl.ACCESS_LOCAL_NETWORK
    const val STATE_RATIONALE_DECLINED = "local_network_rationale_declined"
    const val PREF_PERMISSION_REQUESTED = "local_network_permission_requested"
  }
}
