package com.kelsos.mbrc.networking.protocol

fun interface ProtocolAction {
  suspend fun execute(message: ProtocolMessage)
}
