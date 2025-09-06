package com.kelsos.mbrc.features.settings.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * State holder for license dialog that manages which license to display.
 */
@Stable
class LicenseDialogState {
  // Dialog visibility and content
  var isVisible by mutableStateOf(false)
    private set

  var url by mutableStateOf("")
    private set

  var title by mutableStateOf("")
    private set

  /**
   * Shows the license dialog with the specified content.
   */
  fun showLicense(licenseUrl: String, licenseTitle: String) {
    url = licenseUrl
    title = licenseTitle
    isVisible = true
  }

  /**
   * Hides the license dialog and clears the content.
   */
  fun hideLicense() {
    isVisible = false
    url = ""
    title = ""
  }
}

/**
 * Remember function for LicenseDialogState.
 */
@Composable
fun rememberLicenseDialogState(): LicenseDialogState = remember {
  LicenseDialogState()
}
