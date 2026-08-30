package com.kelsos.mbrc.core.networking

import com.kelsos.mbrc.core.networking.client.PendingCommandBuffer

/**
 * Information about the current reconnection cycle.
 * Used to display progress in the UI during connection attempts.
 */
data class ConnectionCycleInfo(val cycle: Int, val maxCycles: Int)

interface ClientConnectionUseCase {
  /**
   * Makes one connection attempt and reports whether it succeeded. Retrying a failure is the
   * caller's decision, so that only one layer ever schedules attempts.
   */
  suspend fun connect(reset: Boolean = false, cycleInfo: ConnectionCycleInfo? = null): Boolean

  fun disconnect()
}

class ClientConnectionUseCaseImpl(
  private val connectionManager: ClientConnectionManager,
  private val pendingCommands: PendingCommandBuffer
) : ClientConnectionUseCase {
  override suspend fun connect(reset: Boolean, cycleInfo: ConnectionCycleInfo?): Boolean {
    if (reset) {
      connectionManager.stop()
    }
    return connectionManager.connect(cycleInfo)
  }

  override fun disconnect() {
    // Deliberately leaving: buffered commands belong to the session the user just ended, and must
    // not fire against the next one. A reconnect goes through connect(), which keeps them.
    pendingCommands.clear()
    connectionManager.stop()
  }
}
