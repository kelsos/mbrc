package com.kelsos.mbrc.networking.protocol

interface ProtocolAction {
  fun execute(message: ProtocolMessage)
}
