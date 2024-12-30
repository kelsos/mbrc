package com.kelsos.mbrc.ui.preferences

import android.os.Bundle
import android.view.MenuItem
import com.google.android.material.appbar.MaterialToolbar
import com.kelsos.mbrc.R
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.ui.activities.FontActivity
import toothpick.Scope
import toothpick.Toothpick
import toothpick.smoothie.module.SmoothieActivityModule
import javax.inject.Inject

class SettingsActivity : FontActivity() {
  @Inject
  lateinit var bus: RxBus
  private lateinit var scope: Scope

  override fun onCreate(savedInstanceState: Bundle?) {
    scope = Toothpick.openScopes(application, this)
    scope.installModules(SmoothieActivityModule(this))
    super.onCreate(savedInstanceState)
    Toothpick.inject(this, scope)
    setContentView(R.layout.activity_settings)

    val mToolbar = findViewById<MaterialToolbar>(R.id.toolbar)
    setSupportActionBar(mToolbar)
    supportActionBar?.setHomeButtonEnabled(true)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setTitle(R.string.nav_settings)

    val fragment = SettingsFragment.newInstance(bus)
    supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
  }

  override fun onDestroy() {
    Toothpick.closeScope(this)
    super.onDestroy()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
      return true
    }
    return super.onOptionsItemSelected(item)
  }
}
