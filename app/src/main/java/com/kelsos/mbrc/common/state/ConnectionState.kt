package com.kelsos.mbrc.common.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull

interface ConnectionStateFlow {
  val connection: StateFlow<ConnectionStatus>

  suspend fun isConnected(): Boolean
}

interface ConnectionStatePublisher : ConnectionStateFlow {
  suspend fun updateConnection(connection: ConnectionStatus)
}

class ConnectionState : ConnectionStatePublisher {
  private val _connection = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Offline)

  override val connection: StateFlow<ConnectionStatus>
    get() = _connection

  override suspend fun isConnected(): Boolean {
    val connectionStatus = connection.firstOrNull()
    return connectionStatus is ConnectionStatus.Connected
  }

  override suspend fun updateConnection(connection: ConnectionStatus) {
    this._connection.emit(connection)
  }
}
