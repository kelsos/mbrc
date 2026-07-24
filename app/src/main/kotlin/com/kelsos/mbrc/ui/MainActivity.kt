package com.kelsos.mbrc.ui

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.kelsos.mbrc.service.ServiceChecker
import org.koin.android.ext.android.inject

/**
 * Main entry point for the Compose-based MusicBee Remote app.
 * This replaces the traditional fragment-based navigation with Compose Navigation.
 */
class MainActivity : ComponentActivity() {

  private val serviceChecker: ServiceChecker by inject()

  // Start the service once the local-network permission has been resolved (granted or not):
  // a denied result still starts the app, the LAN connection just won't be reachable.
  private val localNetworkPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) {
      serviceChecker.startServiceIfNotRunning()
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    // Install the splash screen before calling super.onCreate()
    installSplashScreen()

    super.onCreate(savedInstanceState)

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
    ensureLocalNetworkAccessThenStart()

    setContent {
      RemoteApp()
    }
  }

  /**
   * On Android 17 (API 37) the local network is gated behind the runtime
   * [ACCESS_LOCAL_NETWORK] permission; without it the TCP/multicast connection to
   * MusicBee is blocked. Request it before starting the service. Older releases and the
   * already-granted case start the service directly.
   */
  private fun ensureLocalNetworkAccessThenStart() {
    if (Build.VERSION.SDK_INT >= LOCAL_NETWORK_PERMISSION_SDK) {
      val alreadyGranted = ContextCompat.checkSelfPermission(this, ACCESS_LOCAL_NETWORK) ==
        PackageManager.PERMISSION_GRANTED
      if (!alreadyGranted) {
        localNetworkPermissionLauncher.launch(ACCESS_LOCAL_NETWORK)
        return
      }
    }
    serviceChecker.startServiceIfNotRunning()
  }

  private companion object {
    // Android 17. Use a literal since the framework constant is not yet named in the SDK.
    const val LOCAL_NETWORK_PERMISSION_SDK = 37
    const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
  }
}
