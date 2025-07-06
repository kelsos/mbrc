package com.kelsos.mbrc.common.state

sealed class ConnectionStatus(val status: String) {
  object Offline : ConnectionStatus("Offline")

  object Authenticating : ConnectionStatus("Authenticating")

  object Connected : ConnectionStatus("Connected")
}
