package com.kelsos.mbrc.common.state

import com.kelsos.mbrc.networking.protocol.Protocol
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerStatus(
  @Json(name = Protocol.PLAYER_MUTE)
  val mute: Boolean,
  @Json(name = Protocol.PLAYER_STATE)
  val playState: String,
  @Json(name = Protocol.PLAYER_REPEAT)
  val repeat: String,
  @Json(name = Protocol.PLAYER_SHUFFLE)
  val shuffle: String,
  @Json(name = Protocol.PLAYER_SCROBBLE)
  val scrobbling: Boolean,
  @Json(name = Protocol.PLAYER_VOLUME)
  val volume: Int
)
