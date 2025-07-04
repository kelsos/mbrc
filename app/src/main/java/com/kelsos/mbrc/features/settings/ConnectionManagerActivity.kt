package com.kelsos.mbrc.features.settings

import android.os.Bundle
import android.widget.Button
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.CommonToolbarActivity
import com.kelsos.mbrc.R
import com.kelsos.mbrc.networking.discovery.DiscoveryStop
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ConnectionManagerActivity :
  CommonToolbarActivity(R.layout.activity_connection_manager),
  SettingsDialogFragment.SettingsSaveListener,
  ConnectionChangeListener {
  private val viewModel: ConnectionManagerViewModel by viewModel()

  private lateinit var recyclerView: RecyclerView
  private lateinit var scanButton: Button

  private val adapter: ConnectionAdapter = ConnectionAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    recyclerView = findViewById(R.id.connection_list)
    scanButton = findViewById(R.id.connection_scan)
    findViewById<Button>(R.id.connection_add).setOnClickListener {
      val settingsDialog = SettingsDialogFragment()
      settingsDialog.show(this.supportFragmentManager, "settings_dialog")
    }
    scanButton.setOnClickListener {
      findViewById<LinearProgressIndicator>(R.id.connection_manager__progress).isGone = false
      scanButton.isEnabled = false
      viewModel.actions.startDiscovery()
    }

    recyclerView.setHasFixedSize(true)
    val layoutManager = LinearLayoutManager(this)
    recyclerView.layoutManager = layoutManager
    adapter.setChangeListener(this)
    recyclerView.adapter = adapter
    setToolbarTitle(R.string.connection_manager_title)

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.settings.collect {
          adapter.submitData(it)
        }
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.events.collect {
          onDiscoveryStopped(it)
        }
      }
    }
  }

  override fun onSave(settings: ConnectionSettings) {
    viewModel.actions.save(settings)
  }

  private fun onDiscoveryStopped(event: DiscoveryStop) {
    findViewById<LinearProgressIndicator>(R.id.connection_manager__progress).isGone = true
    scanButton.isEnabled = true

    val message: String =
      when (event) {
        DiscoveryStop.NoWifi -> getString(R.string.con_man_no_wifi)
        DiscoveryStop.NotFound -> getString(R.string.con_man_not_found)
        is DiscoveryStop.Complete -> {
          getString(R.string.con_man_success)
        }
      }

    Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show()
  }

  override fun onDelete(settings: ConnectionSettings) {
    viewModel.actions.delete(settings)
  }

  override fun onEdit(settings: ConnectionSettings) {
    val settingsDialog = SettingsDialogFragment.newInstance(settings)
    val fragmentManager = supportFragmentManager
    settingsDialog.show(fragmentManager, "settings_dialog")
  }

  override fun onDefault(settings: ConnectionSettings) {
    viewModel.actions.setDefault(settings)
  }
}
