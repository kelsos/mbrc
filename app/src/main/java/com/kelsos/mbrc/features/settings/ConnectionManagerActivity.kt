package com.kelsos.mbrc.features.settings

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.R
import com.kelsos.mbrc.constants.UserInputEventType
import com.kelsos.mbrc.events.DefaultSettingsChangedEvent
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.ConnectionSettingsChanged
import com.kelsos.mbrc.events.ui.DiscoveryStopped
import com.kelsos.mbrc.events.ui.NotifyUser
import com.kelsos.mbrc.networking.discovery.DiscoveryStop
import org.koin.android.ext.android.inject
import org.koin.androidx.scope.ScopeActivity

class ConnectionManagerActivity :
  ScopeActivity(),
  ConnectionManagerView,
  SettingsDialogFragment.SettingsSaveListener,
  ConnectionAdapter.ConnectionChangeListener {
  private val bus: RxBus by inject()
  private val presenter: ConnectionManagerPresenter by inject()

  private lateinit var recyclerView: RecyclerView
  private lateinit var toolbar: MaterialToolbar

  private var adapter: ConnectionAdapter? = null

  private fun onAddButtonClick() {
    val settingsDialog = SettingsDialogFragment()
    settingsDialog.show(supportFragmentManager, "settings_dialog")
  }

  private fun onScanButtonClick() {
    findViewById<LinearProgressIndicator>(R.id.connection_manager__progress).isGone = false
    bus.post(MessageEvent(UserInputEventType.START_DISCOVERY))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.ui_activity_connection_manager)
    recyclerView = findViewById(R.id.connection_list)
    toolbar = findViewById(R.id.toolbar)
    findViewById<Button>(R.id.connection_add).setOnClickListener { onAddButtonClick() }
    findViewById<Button>(R.id.connection_scan).setOnClickListener { onScanButtonClick() }

    setSupportActionBar(toolbar)
    recyclerView.setHasFixedSize(true)
    val layoutManager = LinearLayoutManager(this)
    recyclerView.layoutManager = layoutManager
    adapter = ConnectionAdapter()
    adapter!!.setChangeListener(this)
    recyclerView.adapter = adapter
    presenter.attach(this)
    presenter.load()
  }

  override fun onStart() {
    super.onStart()

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setTitle(R.string.connection_manager_title)
  }

  override fun onResume() {
    super.onResume()
    presenter.attach(this)
    bus.register(
      this,
      ConnectionSettingsChanged::class.java,
      { this.onConnectionSettingsChange(it) },
      true,
    )
    bus.register(this, DiscoveryStopped::class.java, { this.onDiscoveryStopped(it) }, true)
    bus.register(this, NotifyUser::class.java, { this.onUserNotification(it) }, true)
  }

  override fun onPause() {
    super.onPause()
    presenter.detach()
    bus.unregister(this)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> onBackPressedDispatcher.onBackPressed()
      else -> return false
    }
    return true
  }

  override fun onSave(settings: ConnectionSettings) {
    presenter.save(settings)
  }

  private fun onConnectionSettingsChange(event: ConnectionSettingsChanged) {
    adapter!!.setSelectionId(event.defaultId)
  }

  private fun onDiscoveryStopped(event: DiscoveryStopped) {
    findViewById<LinearProgressIndicator>(R.id.connection_manager__progress).isGone = true

    val message: String =
      when (event.reason) {
        DiscoveryStop.NO_WIFI -> getString(R.string.con_man_no_wifi)
        DiscoveryStop.NOT_FOUND -> getString(R.string.con_man_not_found)
        DiscoveryStop.COMPLETE -> {
          presenter.load()
          getString(R.string.con_man_success)
        }
      }

    Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show()
  }

  private fun onUserNotification(event: NotifyUser) {
    val message = if (event.isFromResource) getString(event.resId) else event.message
    Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show()
  }

  override fun onDelete(settings: ConnectionSettings) {
    presenter.delete(settings)
  }

  override fun onEdit(settings: ConnectionSettings) {
    val settingsDialog = SettingsDialogFragment.newInstance(settings)
    val fragmentManager = supportFragmentManager
    settingsDialog.show(fragmentManager, "settings_dialog")
  }

  override fun onDefault(settings: ConnectionSettings) {
    presenter.setDefault(settings)
  }

  override fun updateModel(connectionModel: ConnectionModel) {
    adapter!!.update(connectionModel)
  }

  override fun defaultChanged() {
    bus.post(DefaultSettingsChangedEvent.create())
  }

  override fun dataUpdated() {
    presenter.load()
  }
}
