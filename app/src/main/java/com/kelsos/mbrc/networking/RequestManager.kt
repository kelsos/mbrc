package com.kelsos.mbrc.networking

import com.kelsos.mbrc.networking.client.SocketMessage

interface RequestManager {
  suspend fun openConnection(handshake: Boolean = true): ActiveConnection
  suspend fun request(connection: ActiveConnection, message: SocketMessage): String
}
