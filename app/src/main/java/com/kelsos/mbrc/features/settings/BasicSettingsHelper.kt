package com.kelsos.mbrc.features.settings

import android.content.SharedPreferences
import android.content.res.Resources
import com.kelsos.mbrc.R

class BasicSettingsHelper(
  private val preferences: SharedPreferences,
  private var resources: Resources,
) {
  val defaultAction: String
    get() {
      val key = resources.getString(R.string.preferences_library_track_default_action_key)
      val defaultValue =
        resources.getString(R.string.preferences_library_track_default_action_default_value)
      return preferences.getString(key, defaultValue) ?: defaultValue
    }
}
