package com.kelsos.mbrc

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.common.state.ConnectionStatus
import com.kelsos.mbrc.features.help.HelpFeedbackActivity
import com.kelsos.mbrc.features.library.LibraryActivity
import com.kelsos.mbrc.features.lyrics.LyricsActivity
import com.kelsos.mbrc.features.nowplaying.NowPlayingActivity
import com.kelsos.mbrc.features.output.OutputSelectionDialog
import com.kelsos.mbrc.features.player.PlayerActivity
import com.kelsos.mbrc.features.playlists.PlaylistActivity
import com.kelsos.mbrc.features.radio.RadioActivity
import com.kelsos.mbrc.features.settings.SettingsActivity
import com.kelsos.mbrc.networking.ClientConnectionUseCase
import com.kelsos.mbrc.networking.client.UiMessage
import com.kelsos.mbrc.networking.client.UiMessageQueue
import com.kelsos.mbrc.networking.protocol.VolumeModifyUseCase
import com.kelsos.mbrc.platform.RemoteService
import com.kelsos.mbrc.platform.ServiceChecker
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.scope.ScopeActivity
import timber.log.Timber

private const val NAVIGATION_DELAY = 250L

abstract class BaseActivity(
  @LayoutRes val contentLayoutId: Int,
) : ScopeActivity(contentLayoutId),
  NavigationView.OnNavigationItemSelectedListener {
  private val serviceChecker: ServiceChecker by inject()
  private val connectionUseCase: ClientConnectionUseCase by inject()
  private val volumeModifyUseCase: VolumeModifyUseCase by inject()
  private val connectionState: ConnectionStateFlow by inject()
  private val uiMessageQueue: UiMessageQueue by inject()

  private lateinit var toolbar: MaterialToolbar
  private lateinit var drawer: DrawerLayout
  lateinit var navigationView: NavigationView

  private lateinit var connectText: TextView
  private lateinit var toggle: ActionBarDrawerToggle
  private lateinit var connect: ImageView

  protected abstract fun active(): Int

  private fun onConnectLongClick(): Boolean {
    Timber.v("Connect long pressed")
    serviceChecker.startServiceIfNotRunning()
    connectionUseCase.connect()
    return true
  }

  protected fun onConnectClick() {
    Timber.v("Attempting to connect")
    serviceChecker.startServiceIfNotRunning()
    connectionUseCase.connect()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setupBackButtonHandler()

    toolbar = findViewById(R.id.toolbar)
    drawer = findViewById(R.id.drawer_layout)
    navigationView = findViewById(R.id.nav_view)

    setSupportActionBar(toolbar)

    toggle =
      ActionBarDrawerToggle(this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close)
    drawer.addDrawerListener(toggle)
    toggle.syncState()
    navigationView.setNavigationItemSelectedListener(this)

    val header = navigationView.getHeaderView(0)
    connectText = header.findViewById(R.id.nav_connect_text)
    connect = header.findViewById(R.id.connect_button)
    connect.setOnClickListener { this.onConnectClick() }
    connect.setOnLongClickListener { this.onConnectLongClick() }

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setHomeButtonEnabled(true)
    navigationView.setCheckedItem(active())
    serviceChecker.startServiceIfNotRunning()

    observeFlows()
  }

  override fun onDestroy() {
    super.onDestroy()
    drawer.removeDrawerListener(toggle)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    toggle.onConfigurationChanged(newConfig)
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    toggle.syncState()
  }

  override fun onKeyUp(
    keyCode: Int,
    event: KeyEvent,
  ): Boolean =
    when (keyCode) {
      KeyEvent.KEYCODE_VOLUME_UP -> true
      KeyEvent.KEYCODE_VOLUME_DOWN -> true
      else -> super.onKeyUp(keyCode, event)
    }

  private fun updateConnectionState(event: ConnectionStatus) {
    Timber.v("Handling new connection status %s", event.status)
    @StringRes val resId: Int

    @ColorRes val colorId: Int
    when (event) {
      ConnectionStatus.Offline -> {
        resId = R.string.drawer_connection_status_off
        colorId = R.color.black
      }

      ConnectionStatus.Authenticating -> {
        resId = R.string.drawer_connection_status_on
        colorId = R.color.accent
      }

      ConnectionStatus.Connected -> {
        resId = R.string.drawer_connection_status_active
        colorId = R.color.power_on
      }
    }

    connectText.setText(resId)
    connect.setColorFilter(ContextCompat.getColor(this, colorId))
  }

  override fun onKeyDown(
    keyCode: Int,
    event: KeyEvent,
  ): Boolean {
    val result =
      when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> {
          lifecycleScope.launch {
            volumeModifyUseCase.increase()
          }
          true
        }

        KeyEvent.KEYCODE_VOLUME_DOWN -> {
          lifecycleScope.launch {
            volumeModifyUseCase.decrease()
          }
          true
        }

        else -> super.onKeyDown(keyCode, event)
      }
    return result
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    val itemId = item.itemId
    drawer.closeDrawer(GravityCompat.START)
    drawer.postDelayed({ navigate(itemId) }, NAVIGATION_DELAY)
    return true
  }

  internal fun navigate(itemId: Int) {
    if (active() == itemId) {
      return
    }

    when (itemId) {
      R.id.nav_home -> {
        val upIntent = getParentActivityIntent()
        if (upIntent == null || shouldUpRecreateTask(upIntent)) {
          createBackStack(Intent(this, PlayerActivity::class.java))
        } else {
          navigateUpTo(upIntent)
        }
      }

      R.id.nav_library -> {
        createBackStack(Intent(this, LibraryActivity::class.java))
      }

      R.id.nav_now_playing -> {
        createBackStack(Intent(this, NowPlayingActivity::class.java))
      }

      R.id.nav_playlists -> {
        createBackStack(Intent(this, PlaylistActivity::class.java))
      }

      R.id.nav_radio -> {
        createBackStack(Intent(this, RadioActivity::class.java))
      }

      R.id.nav_lyrics -> {
        createBackStack(Intent(this, LyricsActivity::class.java))
      }

      R.id.nav_settings -> {
        createBackStack(Intent(this, SettingsActivity::class.java))
      }

      R.id.nav_help -> {
        createBackStack(Intent(this, HelpFeedbackActivity::class.java))
      }

      R.id.nav_output -> {
        OutputSelectionDialog.create(supportFragmentManager).show()
      }

      R.id.nav_exit -> {
        exitApplication()
      }
    }
  }

  internal fun exitApplication() {
    if (!RemoteService.serviceStopping) {
      val intent = Intent(this, RemoteService::class.java)
      stopService(intent)
    }

    if (this is PlayerActivity) {
      finish()
    } else {
      val intent = Intent(this, PlayerActivity::class.java)
      intent.putExtra(EXIT_APP, true)
      intent.flags = FLAG_ACTIVITY_CLEAR_TOP
      startActivity(intent)
    }
  }

  private fun createBackStack(intent: Intent) {
    val builder = TaskStackBuilder.create(this)
    builder.addNextIntentWithParentStack(intent)
    builder.startActivities()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in, R.anim.slide_out)
    } else {
      @Suppress("DEPRECATION")
      overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
    }
  }

  private fun observeFlows() {
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        connectionState.connection.collect { event ->
          updateConnectionState(event)
        }
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        uiMessageQueue.messages.collect {
          if (it is UiMessage.PluginUpdateRequired) {
            showPluginUpdateRequired(it.minimumVersion)
          } else if (it is UiMessage.PluginUpdateAvailable) {
            showPluginUpdateAvailable()
          }
        }
      }
    }
  }

  fun showPluginUpdateRequired(minimumRequired: String) {
    val intent = Intent(this, UpdateRequiredActivity::class.java)
    intent.putExtra(UpdateRequiredActivity.VERSION, minimumRequired)
    startActivity(intent)
  }

  fun showPluginUpdateAvailable() {
    val snackBar =
      Snackbar.make(
        navigationView,
        R.string.main__dialog_plugin_outdated_message,
        Snackbar.LENGTH_INDEFINITE,
      )
    snackBar.setAction(android.R.string.ok) { snackBar.dismiss() }
    snackBar.show()
  }

  private fun setupBackButtonHandler() {
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
          } else {
            finish()
          }
        }
      },
    )
  }

  companion object {
    const val EXIT_APP = "mbrc.exit"
  }
}
