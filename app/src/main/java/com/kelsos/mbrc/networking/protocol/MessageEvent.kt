package com.kelsos.mbrc.networking.protocol

data class MessageEvent(override val type: Protocol, override val data: Any = "") :
  ProtocolMessage {
  override fun toString(): String = "{context=${type.context}, data=$data}"
}
