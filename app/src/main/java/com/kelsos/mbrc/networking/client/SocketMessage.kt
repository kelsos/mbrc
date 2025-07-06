package com.kelsos.mbrc.networking.client

import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.ProtocolPayload
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SocketMessage(
  @Json(name = "context")
  val context: String,
  @Json(name = "data")
  val data: Any = ""
) {
  override fun toString(): String = "{ context=$context, data=$data }"

  companion object {
    fun create(protocol: Protocol, data: Any = ""): SocketMessage =
      SocketMessage(protocol.context, data)
  }
}

fun SocketMessage.Companion.player(): SocketMessage = create(Protocol.Player, "Android")

fun SocketMessage.Companion.protocol(payload: ProtocolPayload): SocketMessage =
  create(Protocol.ProtocolTag, payload)
