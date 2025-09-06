package com.kelsos.mbrc.features.settings.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.features.settings.ConnectionManagerViewModel
import com.kelsos.mbrc.features.settings.ConnectionSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * State holder for ConnectionManagerScreen that manages dialog states and UI logic.
 */
@Stable
class ConnectionManagerScreenState(
  private val viewModel: ConnectionManagerViewModel,
  private val context: Context,
  private val appDispatchers: AppCoroutineDispatchers
) {
  // Coroutine scope for async operations
  private val scope = CoroutineScope(SupervisorJob() + appDispatchers.main)

  // Dialog state
  var showAddDialog: Boolean by mutableStateOf(false)
    private set

  var editingConnection: ConnectionSettings? by mutableStateOf(null)
    private set

  // FAB state
  var isFabMenuExpanded: Boolean by mutableStateOf(false)
    private set

  // Scanning state
  var isScanning: Boolean by mutableStateOf(false)
    private set

  /**
   * Shows the add connection dialog.
   */
  fun showAddDialog() {
    showAddDialog = true
    isFabMenuExpanded = false
  }

  /**
   * Shows the edit connection dialog.
   */
  fun showEditDialog(connection: ConnectionSettings) {
    editingConnection = connection
    isFabMenuExpanded = false
  }

  /**
   * Hides any visible dialog.
   */
  fun hideDialog() {
    showAddDialog = false
    editingConnection = null
  }

  /**
   * Toggles the FAB menu expansion.
   */
  fun toggleFabMenu() {
    isFabMenuExpanded = !isFabMenuExpanded
  }

  /**
   * Starts network scanning.
   */
  fun startScanning() {
    isScanning = true
    isFabMenuExpanded = false
    viewModel.actions.startDiscovery()
  }

  /**
   * Stops network scanning.
   */
  fun stopScanning() {
    isScanning = false
  }

  /**
   * Saves a connection and closes the dialog.
   */
  fun saveConnection(connection: ConnectionSettings) {
    scope.launch {
      viewModel.actions.save(connection)
      hideDialog()
    }
  }

  /**
   * Deletes a connection.
   */
  fun deleteConnection(connection: ConnectionSettings) {
    scope.launch {
      viewModel.actions.delete(connection)
    }
  }

  /**
   * Sets a connection as default.
   */
  fun setDefaultConnection(connection: ConnectionSettings) {
    scope.launch {
      viewModel.actions.setDefault(connection)
    }
  }
}

/**
 * Remember function for ConnectionManagerScreenState that handles dependency injection.
 */
@Composable
fun rememberConnectionManagerScreenState(
  viewModel: ConnectionManagerViewModel = koinInject(),
  context: Context = LocalContext.current,
  appDispatchers: AppCoroutineDispatchers = koinInject()
): ConnectionManagerScreenState = remember(viewModel, context, appDispatchers) {
  ConnectionManagerScreenState(viewModel, context, appDispatchers)
}
