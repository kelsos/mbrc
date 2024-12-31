package com.kelsos.mbrc.networking.protocol

interface ProtocolMessage {
  val type: String

  val data: Any

  val dataString: String
}
