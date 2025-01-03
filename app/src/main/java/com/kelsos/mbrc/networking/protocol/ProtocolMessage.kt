package com.kelsos.mbrc.networking.protocol

interface ProtocolMessage {
  val type: Protocol

  val data: Any
}
