package com.kelsos.mbrc.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.core.common.state.ConnectionStateFlow
import com.kelsos.mbrc.core.common.state.ConnectionStatePublisher
import com.kelsos.mbrc.core.common.state.ConnectionStatus
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.networking.ClientConnectionUseCase
import com.kelsos.mbrc.core.networking.LocalNetworkAccess
import com.kelsos.mbrc.feature.settings.domain.ConnectionRepository
import com.kelsos.mbrc.service.ServiceChecker
import com.kelsos.mbrc.service.ServiceLifecycleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DrawerViewModel(
  private val connectionStateFlow: ConnectionStateFlow,
  private val clientConnectionUseCase: ClientConnectionUseCase,
  private val connectionRepository: ConnectionRepository,
  private val dispatchers: AppCoroutineDispatchers,
  private val serviceChecker: ServiceChecker,
  private val serviceLifecycleManager: ServiceLifecycleManager,
  private val localNetworkAccess: LocalNetworkAccess,
  private val connectionStatePublisher: ConnectionStatePublisher
) : ViewModel() {

  val connectionStatus: StateFlow<ConnectionStatus> = connectionStateFlow.connection

  private val _connectionName = MutableStateFlow<String?>(null)
  val connectionName: StateFlow<String?> = _connectionName.asStateFlow()

  init {
    observeConnectionStatus()
  }

  private fun observeConnectionStatus() {
    viewModelScope.launch {
      connectionStatus.collect { status ->
        if (status is ConnectionStatus.Connected) {
          loadConnectionName()
        } else {
          _connectionName.value = null
        }
      }
    }
  }

  private suspend fun loadConnectionName() {
    val default = withContext(dispatchers.database) {
      connectionRepository.getDefault()
    } ?: return
    _connectionName.value = default.name.ifBlank { "${default.address}:${default.port}" }
  }

  fun isConnected(): Boolean = connectionStatus.value is ConnectionStatus.Connected

  /**
   * Disconnects only from [ConnectionStatus.Connected]; every other state starts a fresh attempt.
   *
   * A tap while connecting used to disconnect and stop the service, so the one gesture the user has
   * for reaching MusicBee moved them further from it. Restarting instead means that whenever the
   * network is up and the plugin is listening, tapping converges on a connection.
   *
   * Returns false when local network access is denied, so the caller can ask for access rather than
   * showing an attempt that cannot succeed.
   */
  fun toggleConnection(): Boolean {
    val disconnecting = isConnected()
    if (!disconnecting && !localNetworkAccess.isPermitted()) {
      connectionStatePublisher.updateConnection(ConnectionStatus.LocalNetworkDenied)
      return false
    }
    viewModelScope.launch {
      if (disconnecting) {
        serviceLifecycleManager.onIntentionalDisconnect()
        clientConnectionUseCase.disconnect()
      } else {
        serviceLifecycleManager.onManualReconnect()
        serviceChecker.startServiceIfNotRunning()
        clientConnectionUseCase.connect(reset = true)
      }
    }
    return true
  }

  /**
   * Re-checks access after the user has been through the permission flow.
   *
   * Connects rather than just clearing the state: the user granted access in order to reach
   * MusicBee, and the service was never started while access was denied, so leaving them at "not
   * connected" would make them tap connect for no reason. Publishing Offline instead would also be
   * read as a connection loss and buy a 15s reconnection delay before the first attempt.
   */
  fun refreshLocalNetworkAccess() {
    if (!localNetworkAccess.isPermitted()) {
      return
    }
    if (connectionStatus.value !is ConnectionStatus.LocalNetworkDenied) {
      return
    }
    viewModelScope.launch {
      serviceChecker.startServiceIfNotRunning()
      clientConnectionUseCase.connect()
    }
  }
}
