package com.kelsos.mbrc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat

/**
 * Main entry point for the Compose-based MusicBee Remote app.
 * This replaces the traditional fragment-based navigation with Compose Navigation.
 */
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    // Install the splash screen before calling super.onCreate()
    installSplashScreen()

    super.onCreate(savedInstanceState)

    // Enable edge-to-edge display for modern Android design
    enableEdgeToEdge()

    // Allow content to draw behind system bars
    WindowCompat.setDecorFitsSystemWindows(window, false)

    setContent {
      RemoteApp()
    }
  }
}
