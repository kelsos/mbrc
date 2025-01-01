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
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.annotations.Connection
import com.kelsos.mbrc.constants.UserInputEventType
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.ConnectionStatusChangeEvent
import com.kelsos.mbrc.events.ui.NotifyUser
import com.kelsos.mbrc.events.ui.RequestConnectionStateEvent
import com.kelsos.mbrc.features.help.HelpFeedbackActivity
import com.kelsos.mbrc.features.library.LibraryActivity
import com.kelsos.mbrc.features.lyrics.LyricsActivity
import com.kelsos.mbrc.features.nowplaying.NowPlayingActivity
import com.kelsos.mbrc.features.output.OutputSelectionDialog
import com.kelsos.mbrc.features.player.PlayerActivity
import com.kelsos.mbrc.features.playlists.PlaylistActivity
import com.kelsos.mbrc.features.radio.RadioActivity
import com.kelsos.mbrc.features.settings.SettingsActivity
import com.kelsos.mbrc.platform.RemoteService
import com.kelsos.mbrc.platform.ServiceChecker
import org.koin.android.ext.android.inject
import org.koin.androidx.scope.ScopeActivity
import timber.log.Timber

abstract class BaseActivity :
  ScopeActivity(),
  NavigationView.OnNavigationItemSelectedListener {
  private val bus: RxBus by inject()
  private val serviceChecker: ServiceChecker by inject()

  private lateinit var toolbar: MaterialToolbar
  private lateinit var drawer: DrawerLayout
  lateinit var navigationView: NavigationView

  private var connectText: TextView? = null
  private var toggle: ActionBarDrawerToggle? = null
  private var connect: ImageView? = null

  protected abstract fun active(): Int

  protected var isConnected: Boolean = false

  private fun onConnectLongClick(): Boolean {
    Timber.v("Connect long pressed")
    serviceChecker.startServiceIfNotRunning()
    bus.post(MessageEvent(UserInputEventType.RESET_CONNECTION))
    return true
  }

  protected fun onConnectClick() {
    Timber.v("Attempting to connect")
    serviceChecker.startServiceIfNotRunning()
    bus.post(MessageEvent(UserInputEventType.START_CONNECTION))
  }

  override fun onDestroy() {
    super.onDestroy()
    drawer.removeDrawerListener(toggle!!)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    toggle!!.onConfigurationChanged(newConfig)
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    toggle!!.syncState()
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

  private fun onConnection(event: ConnectionStatusChangeEvent) {
    Timber.v("Handling new connection status %s", event.status)
    @StringRes val resId: Int

    @ColorRes val colorId: Int
    if (event.status == Connection.OFF) {
      resId = R.string.drawer_connection_status_off
      colorId = R.color.black
    } else if (event.status == Connection.ON) {
      resId = R.string.drawer_connection_status_on
      colorId = R.color.accent
    } else if (event.status == Connection.ACTIVE) {
      resId = R.string.drawer_connection_status_active
      colorId = R.color.power_on
    } else {
      resId = R.string.drawer_connection_status_off
      colorId = R.color.black
    }

    connectText!!.setText(resId)
    connect!!.setColorFilter(ContextCompat.getColor(this, colorId))
    isConnected = event.status == Connection.ACTIVE
  }

  private fun handleUserNotification(event: NotifyUser) {
    val message = if (event.isFromResource) getString(event.resId) else event.message

    val focus = currentFocus
    if (focus != null) {
      Snackbar.make(focus, message, Snackbar.LENGTH_SHORT).show()
    }
  }

  override fun onKeyDown(
    keyCode: Int,
    event: KeyEvent,
  ): Boolean {
    when (keyCode) {
      KeyEvent.KEYCODE_VOLUME_UP -> {
        bus.post(MessageEvent(UserInputEventType.KEY_VOLUME_UP))
        return true
      }

      KeyEvent.KEYCODE_VOLUME_DOWN -> {
        bus.post(MessageEvent(UserInputEventType.KEY_VOLUME_DOWN))
        return true
      }

      else -> return super.onKeyDown(keyCode, event)
    }
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    val itemId = item.itemId
    drawer.closeDrawer(GravityCompat.START)
    drawer.postDelayed({ navigate(itemId) }, 250)
    return true
  }

  internal fun navigate(itemId: Int) {
    if (active() == itemId) {
      return
    }

    if (itemId == R.id.nav_home) {
      val upIntent = getParentActivityIntent()
      if (upIntent == null || shouldUpRecreateTask(upIntent)) {
        createBackStack(Intent(this, PlayerActivity::class.java))
      } else {
        navigateUpTo(upIntent)
      }
    } else if (itemId == R.id.nav_library) {
      createBackStack(Intent(this, LibraryActivity::class.java))
    } else if (itemId == R.id.nav_now_playing) {
      createBackStack(Intent(this, NowPlayingActivity::class.java))
    } else if (itemId == R.id.nav_playlists) {
      createBackStack(Intent(this, PlaylistActivity::class.java))
    } else if (itemId == R.id.nav_radio) {
      createBackStack(Intent(this, RadioActivity::class.java))
    } else if (itemId == R.id.nav_lyrics) {
      createBackStack(Intent(this, LyricsActivity::class.java))
    } else if (itemId == R.id.nav_settings) {
      createBackStack(Intent(this, SettingsActivity::class.java))
    } else if (itemId == R.id.nav_help) {
      createBackStack(Intent(this, HelpFeedbackActivity::class.java))
    } else if (itemId == R.id.nav_output) {
      OutputSelectionDialog.create(supportFragmentManager).show()
    } else if (itemId == R.id.nav_exit) {
      exitApplication()
    }
  }

  internal fun exitApplication() {
    if (!RemoteService.serviceStopping) {
      stopService(Intent(this, RemoteService::class.java))
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

  /**
   * Should be called after injections and setContentView.
   */
  fun setup() {
    Timber.v("Initializing base activity")
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
          } else {
            onBackPressedDispatcher.onBackPressed()
          }
        }
      },
    )
    toolbar = findViewById(R.id.toolbar)
    drawer = findViewById(R.id.drawer_layout)
    navigationView = findViewById(R.id.nav_view)

    setSupportActionBar(toolbar)

    toggle =
      ActionBarDrawerToggle(this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close)
    drawer.addDrawerListener(toggle!!)
    drawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
    toggle!!.syncState()
    navigationView.setNavigationItemSelectedListener(this)

    val header = navigationView.getHeaderView(0)
    connectText = header.findViewById(R.id.nav_connect_text)
    connect = header.findViewById(R.id.connect_button)
    connect!!.setOnClickListener { this.onConnectClick() }
    connect!!.setOnLongClickListener { this.onConnectLongClick() }

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setHomeButtonEnabled(true)
    navigationView.setCheckedItem(active())
    serviceChecker.startServiceIfNotRunning()
  }

  override fun onResume() {
    super.onResume()
    this.bus.register(this, NotifyUser::class.java, { this.handleUserNotification(it) }, true)
    this.bus.register(
      this,
      ConnectionStatusChangeEvent::class.java,
      { this.onConnection(it) },
      true,
    )
    this.bus.post(RequestConnectionStateEvent())
  }

  override fun onPause() {
    super.onPause()
    this.bus.unregister(this)
  }

  companion object {
    const val EXIT_APP = "mbrc.exit"
  }
}
