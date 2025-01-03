package com.kelsos.mbrc.networking

import com.kelsos.mbrc.networking.client.ClientConnectionManager

interface ClientConnectionUseCase {
  fun connect(reset: Boolean = false)
}

class ClientConnectionUseCaseImpl(
  private val connectionManager: ClientConnectionManager,
) : ClientConnectionUseCase {
  override fun connect(reset: Boolean) {
    if (reset) {
      connectionManager.stop()
    }
    connectionManager.start()
  }
}
