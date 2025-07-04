package com.kelsos.mbrc.features.settings

import android.os.Bundle
import com.kelsos.mbrc.CommonToolbarActivity
import com.kelsos.mbrc.R

class SettingsActivity : CommonToolbarActivity(R.layout.activity_settings) {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    supportActionBar?.setHomeButtonEnabled(true)
    setToolbarTitle(R.string.nav_settings)

    val fragment = SettingsFragment.newInstance()
    supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
  }
}
