package com.kelsos.mbrc.mock.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

val json = Json {
  ignoreUnknownKeys = true
  encodeDefaults = true
  isLenient = true
}

@Serializable
data class SocketMessage(
  val context: String,
  val data: JsonElement
)

@Serializable
data class ProtocolPayload(
  @SerialName("client_id")
  val clientId: String,
  @SerialName("no_broadcast")
  val noBroadcast: Boolean = false,
  @SerialName("protocol_version")
  val protocolVersion: Int = Protocol.PROTOCOL_VERSION
)

@Serializable
data class PlayerStatus(
  @SerialName("playState")
  val playState: String,
  val repeat: String,
  val shuffle: String,
  val mute: Boolean,
  val scrobbling: Boolean,
  val volume: Int
)

@Serializable
data class NowPlayingTrack(
  val artist: String,
  val title: String,
  val album: String,
  val year: String,
  val path: String
)

@Serializable
data class NowPlayingPosition(
  val current: Long,
  val total: Long
)

@Serializable
data class CoverPayload(
  val status: String,
  val cover: String = ""
)

@Serializable
data class LyricsPayload(
  val status: String,
  val lyrics: String = ""
)

@Serializable
data class DiscoveryMessage(
  val name: String,
  val address: String,
  val port: Int,
  val context: String
)

@Serializable
data class TrackDto(
  val artist: String = "",
  val title: String = "",
  val src: String = "",
  val trackno: Int = 0,
  val disc: Int = 0,
  @SerialName("album_artist")
  val albumArtist: String = "",
  val album: String = "",
  val genre: String = "",
  val year: String = ""
)

@Serializable
data class ArtistDto(val artist: String = "")

@Serializable
data class AlbumDto(
  val artist: String = "",
  val album: String = ""
)

@Serializable
data class GenreDto(val genre: String = "")

@Serializable
data class RadioStationDto(val name: String, val url: String)

@Serializable
data class PlaylistDto(val name: String, val url: String)

@Serializable
data class PagedData<T>(
  val offset: Int,
  val limit: Int,
  val total: Int,
  val data: List<T>
)

@Serializable
data class OutputDevice(
  val name: String,
  val active: Boolean
)
