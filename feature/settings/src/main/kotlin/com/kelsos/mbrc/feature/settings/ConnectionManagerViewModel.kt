package com.kelsos.mbrc.feature.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kelsos.mbrc.core.common.data.ConnectionSettings
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.networking.ClientConnectionUseCase
import com.kelsos.mbrc.core.networking.discovery.DiscoveryStop
import com.kelsos.mbrc.feature.settings.domain.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dialog state for the connection manager.
 */
@Stable
sealed interface ConnectionDialogState {
  data object Hidden : ConnectionDialogState
  data object Add : ConnectionDialogState
  data class Edit(val connection: ConnectionSettings) : ConnectionDialogState
}

/**
 * Scanning state for network discovery.
 */
@Stable
sealed interface ScanningState {
  data object Idle : ScanningState
  data object Scanning : ScanningState
}

/**
 * Form state for add/edit connection dialog.
 */
@Stable
data class ConnectionFormState(
  val name: String = "",
  val address: String = "",
  val port: String = "3000",
  val portError: String? = null
) {
  val isValid: Boolean
    get() = address.isNotEmpty() && portError == null

  val portNumber: Int
    get() = port.toIntOrNull() ?: 0
}

/**
 * ViewModel for the Connection Manager screen.
 * Manages all UI state including dialogs, scanning, FAB menu, and form state.
 */
class ConnectionManagerViewModel(
  private val repository: ConnectionRepository,
  private val dispatchers: AppCoroutineDispatchers,
  private val clientConnectionUseCase: ClientConnectionUseCase
) : ViewModel() {

  // Dialog state
  private val _dialogState = MutableStateFlow<ConnectionDialogState>(ConnectionDialogState.Hidden)
  val dialogState: StateFlow<ConnectionDialogState> = _dialogState.asStateFlow()

  // Scanning state
  private val _scanningState = MutableStateFlow<ScanningState>(ScanningState.Idle)
  val scanningState: StateFlow<ScanningState> = _scanningState.asStateFlow()

  // FAB menu state
  private val _fabExpanded = MutableStateFlow(false)
  val fabExpanded: StateFlow<Boolean> = _fabExpanded.asStateFlow()

  // Form state for add/edit dialog
  private val _formState = MutableStateFlow(ConnectionFormState())
  val formState: StateFlow<ConnectionFormState> = _formState.asStateFlow()

  // Discovery events for snackbar messages
  private val _discoveryEvents = MutableSharedFlow<DiscoveryStop>()
  val discoveryEvents: SharedFlow<DiscoveryStop> = _discoveryEvents.asSharedFlow()

  // Connection list
  val connections: Flow<PagingData<ConnectionSettings>> =
    repository.getAll().cachedIn(viewModelScope)

  // Dialog actions

  /**
   * Shows the add connection dialog and resets form to defaults.
   */
  fun showAddDialog() {
    _formState.value = ConnectionFormState()
    _dialogState.value = ConnectionDialogState.Add
    _fabExpanded.value = false
  }

  /**
   * Shows the edit connection dialog and populates form with connection data.
   */
  fun showEditDialog(connection: ConnectionSettings) {
    _formState.value = ConnectionFormState(
      name = connection.name,
      address = connection.address,
      port = connection.port.toString()
    )
    _dialogState.value = ConnectionDialogState.Edit(connection)
    _fabExpanded.value = false
  }

  /**
   * Hides the dialog and clears form state.
   */
  fun hideDialog() {
    _dialogState.value = ConnectionDialogState.Hidden
    _formState.value = ConnectionFormState()
  }

  // FAB actions

  /**
   * Toggles the FAB menu expansion.
   */
  fun toggleFabMenu() {
    _fabExpanded.update { !it }
  }

  /**
   * Collapses the FAB menu.
   */
  fun collapseFabMenu() {
    _fabExpanded.value = false
  }

  // Scanning actions

  /**
   * Starts network discovery for MusicBee plugin.
   */
  fun startScanning() {
    _scanningState.value = ScanningState.Scanning
    _fabExpanded.value = false

    viewModelScope.launch(dispatchers.network) {
      val result = repository.discover()
      _scanningState.value = ScanningState.Idle
      _discoveryEvents.emit(result)
    }
  }

  /**
   * Stops/cancels network scanning.
   */
  fun stopScanning() {
    _scanningState.value = ScanningState.Idle
  }

  // Form actions

  /**
   * Updates the connection name field.
   */
  fun updateName(name: String) {
    _formState.update { it.copy(name = name) }
  }

  /**
   * Updates the connection address field.
   */
  fun updateAddress(address: String) {
    _formState.update { it.copy(address = address) }
  }

  /**
   * Updates the port field and validates it.
   */
  fun updatePort(port: String, errorMessage: String) {
    val portNum = port.toIntOrNull() ?: 0
    val error = if (portNum !in 1..MAX_PORT) errorMessage else null
    _formState.update { it.copy(port = port, portError = error) }
  }

  // Connection actions

  /**
   * Saves the current form state as a connection.
   * For edit mode, updates the existing connection.
   * For add mode, creates a new connection.
   */
  fun saveConnection() {
    val form = _formState.value
    if (!form.isValid || form.portNumber !in 1..MAX_PORT) return

    val dialogState = _dialogState.value
    val baseConnection = when (dialogState) {
      is ConnectionDialogState.Edit -> dialogState.connection
      else -> ConnectionSettings.default()
    }

    val connection = baseConnection.copy(
      name = form.name,
      address = form.address,
      port = form.portNumber
    )

    viewModelScope.launch(dispatchers.database) {
      repository.save(connection)
    }

    hideDialog()
  }

  /**
   * Deletes a connection.
   */
  fun deleteConnection(connection: ConnectionSettings) {
    viewModelScope.launch(dispatchers.database) {
      repository.delete(connection)
    }
  }

  /**
   * Sets a connection as the default.
   */
  fun setDefaultConnection(connection: ConnectionSettings) {
    viewModelScope.launch {
      withContext(dispatchers.database) { repository.setDefault(connection) }
      clientConnectionUseCase.connect(reset = true)
    }
  }

  companion object {
    private const val MAX_PORT = 65535
  }
}
