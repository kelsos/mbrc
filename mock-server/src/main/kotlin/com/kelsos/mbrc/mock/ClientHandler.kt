package com.kelsos.mbrc.mock

import com.kelsos.mbrc.mock.protocol.CoverPayload
import com.kelsos.mbrc.mock.protocol.LyricsPayload
import com.kelsos.mbrc.mock.protocol.NowPlayingPosition
import com.kelsos.mbrc.mock.protocol.OutputDevice
import com.kelsos.mbrc.mock.protocol.PagedData
import com.kelsos.mbrc.mock.protocol.PlayState
import com.kelsos.mbrc.mock.protocol.PlayerStatus
import com.kelsos.mbrc.mock.protocol.Protocol
import com.kelsos.mbrc.mock.protocol.ProtocolPayload
import com.kelsos.mbrc.mock.protocol.RepeatMode
import com.kelsos.mbrc.mock.protocol.ShuffleMode
import com.kelsos.mbrc.mock.protocol.SocketMessage
import com.kelsos.mbrc.mock.protocol.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.Socket

class ClientHandler(
  private val socket: Socket,
  private val state: MockState
) {
  private val reader: BufferedReader = socket.getInputStream().bufferedReader()
  private val writer: PrintWriter = PrintWriter(socket.getOutputStream(), true)
  private var clientId: String = ""
  private var pingJob: Job? = null

  fun handle() {
    try {
      println("[Client ${socket.inetAddress}] Connected")
      handshake()

      // Start ping job
      pingJob = CoroutineScope(Dispatchers.IO).launch {
        while (isActive) {
          delay(20000)
          sendPing()
        }
      }

      // Read and process messages
      while (!socket.isClosed) {
        val line = reader.readLine() ?: break
        processMessage(line)
      }
    } catch (e: Exception) {
      println("[Client ${socket.inetAddress}] Error: ${e.message}")
    } finally {
      pingJob?.cancel()
      socket.close()
      println("[Client ${socket.inetAddress}] Disconnected")
    }
  }

  private fun handshake() {
    // Step 1: Wait for player context from client
    val playerMsg = reader.readLine() ?: throw Exception("No player message received")
    println("[Client] <- $playerMsg")

    // Step 2: Send player status
    send(Protocol.PLAYER, JsonPrimitive(state.playState.value))

    // Step 3: Wait for protocol negotiation
    val protoMsg = reader.readLine() ?: throw Exception("No protocol message received")
    println("[Client] <- $protoMsg")

    val parsed = json.decodeFromString<SocketMessage>(protoMsg)
    if (parsed.context == Protocol.PROTOCOL_TAG) {
      val payload = json.decodeFromJsonElement(ProtocolPayload.serializer(), parsed.data)
      clientId = payload.clientId
      println("[Client] Client ID: $clientId, Protocol: ${payload.protocolVersion}")

      // Step 4: Send protocol version
      send(Protocol.PROTOCOL_TAG, JsonPrimitive(Protocol.PROTOCOL_VERSION))
    }

    // Step 5: Wait for init
    val initMsg = reader.readLine() ?: throw Exception("No init message received")
    println("[Client] <- $initMsg")

    // Send initial state
    sendPlayerStatus()
    sendNowPlayingTrack()
    sendNowPlayingPosition()
  }

  private fun processMessage(line: String) {
    println("[Client] <- $line")
    try {
      val message = json.decodeFromString<SocketMessage>(line)
      handleCommand(message)
    } catch (e: Exception) {
      println("[Client] Failed to parse message: ${e.message}")
    }
  }

  private fun handleCommand(message: SocketMessage) {
    when (message.context) {
      Protocol.PONG -> {
        // Pong received, connection is alive
      }
      Protocol.PLAYER_PLAY_PAUSE -> {
        state.playState = PlayState.toggle(state.playState)
        send(Protocol.PLAYER_STATE, JsonPrimitive(state.playState.value))
      }
      Protocol.PLAYER_PLAY -> {
        state.playState = PlayState.Playing
        send(Protocol.PLAYER_STATE, JsonPrimitive(state.playState.value))
      }
      Protocol.PLAYER_PAUSE -> {
        state.playState = PlayState.Paused
        send(Protocol.PLAYER_STATE, JsonPrimitive(state.playState.value))
      }
      Protocol.PLAYER_STOP -> {
        state.playState = PlayState.Stopped
        state.position = 0
        send(Protocol.PLAYER_STATE, JsonPrimitive(state.playState.value))
      }
      Protocol.PLAYER_NEXT -> {
        state.nextTrack()
        sendNowPlayingTrack()
        sendNowPlayingPosition()
      }
      Protocol.PLAYER_PREVIOUS -> {
        state.previousTrack()
        sendNowPlayingTrack()
        sendNowPlayingPosition()
      }
      Protocol.PLAYER_VOLUME -> {
        val data = message.data.jsonPrimitive.intOrNull
        if (data != null) {
          state.volume = data.coerceIn(0, 100)
        }
        send(Protocol.PLAYER_VOLUME, JsonPrimitive(state.volume))
      }
      Protocol.PLAYER_MUTE -> {
        state.mute = !state.mute
        send(Protocol.PLAYER_MUTE, JsonPrimitive(state.mute))
      }
      Protocol.PLAYER_SHUFFLE -> {
        state.shuffle = ShuffleMode.toggle(state.shuffle)
        send(Protocol.PLAYER_SHUFFLE, JsonPrimitive(state.shuffle.value))
      }
      Protocol.PLAYER_REPEAT -> {
        state.repeat = RepeatMode.toggle(state.repeat)
        send(Protocol.PLAYER_REPEAT, JsonPrimitive(state.repeat.value))
      }
      Protocol.PLAYER_SCROBBLE -> {
        state.scrobbling = !state.scrobbling
        send(Protocol.PLAYER_SCROBBLE, JsonPrimitive(state.scrobbling))
      }
      Protocol.NOW_PLAYING_TRACK -> {
        sendNowPlayingTrack()
      }
      Protocol.NOW_PLAYING_POSITION -> {
        val data = message.data.jsonPrimitive.intOrNull
        if (data != null) {
          state.position = data.toLong()
        }
        sendNowPlayingPosition()
      }
      Protocol.NOW_PLAYING_COVER -> {
        sendNowPlayingCover()
      }
      Protocol.NOW_PLAYING_LYRICS -> {
        sendNowPlayingLyrics()
      }
      Protocol.NOW_PLAYING_RATING -> {
        val data = message.data.jsonPrimitive.content
        if (data != "toggle") {
          state.rating = data.toFloatOrNull() ?: 0f
        }
        send(Protocol.NOW_PLAYING_RATING, JsonPrimitive(state.rating))
      }
      Protocol.NOW_PLAYING_LIST -> {
        sendNowPlayingList(message)
      }
      Protocol.PLAYER_STATUS -> {
        sendPlayerStatus()
      }
      Protocol.PLAYER_OUTPUT -> {
        sendOutputDevices()
      }
      Protocol.LIBRARY_BROWSE_GENRES -> {
        sendLibraryGenres(message)
      }
      Protocol.LIBRARY_BROWSE_ARTISTS -> {
        sendLibraryArtists(message)
      }
      Protocol.LIBRARY_BROWSE_ALBUMS -> {
        sendLibraryAlbums(message)
      }
      Protocol.LIBRARY_BROWSE_TRACKS -> {
        sendLibraryTracks(message)
      }
      Protocol.RADIO_STATIONS -> {
        sendRadioStations(message)
      }
      Protocol.PLAYLIST_LIST -> {
        sendPlaylists(message)
      }
      Protocol.VERIFY_CONNECTION -> {
        send(Protocol.VERIFY_CONNECTION, JsonPrimitive(true))
      }
      else -> {
        println("[Client] Unknown command: ${message.context}")
      }
    }
  }

  private fun send(context: String, data: kotlinx.serialization.json.JsonElement) {
    val msg = json.encodeToString(SocketMessage(context, data))
    writer.println(msg)
    println("[Client] -> $msg")
  }

  private fun sendPing() {
    send(Protocol.PING, JsonPrimitive(""))
  }

  private fun sendPlayerStatus() {
    val status = PlayerStatus(
      playState = state.playState.value,
      repeat = state.repeat.value,
      shuffle = state.shuffle.value,
      mute = state.mute,
      scrobbling = state.scrobbling,
      volume = state.volume
    )
    send(Protocol.PLAYER_STATUS, json.encodeToJsonElement(PlayerStatus.serializer(), status))
  }

  private fun sendNowPlayingTrack() {
    send(
      Protocol.NOW_PLAYING_TRACK,
      json.encodeToJsonElement(
        com.kelsos.mbrc.mock.protocol.NowPlayingTrack.serializer(),
        state.currentTrack
      )
    )
  }

  private fun sendNowPlayingPosition() {
    val pos = NowPlayingPosition(current = state.position, total = state.trackDuration)
    send(Protocol.NOW_PLAYING_POSITION, json.encodeToJsonElement(NowPlayingPosition.serializer(), pos))
  }

  private fun sendNowPlayingCover() {
    // Send empty cover (no actual image data in mock)
    val cover = CoverPayload(status = "ready", cover = "")
    send(Protocol.NOW_PLAYING_COVER, json.encodeToJsonElement(CoverPayload.serializer(), cover))
  }

  private fun sendNowPlayingLyrics() {
    val lyrics = LyricsPayload(
      status = "success",
      lyrics = """
        |[Mock Lyrics]
        |
        |Verse 1:
        |This is a mock server test
        |Playing music at your request
        |
        |Chorus:
        |Mock server, mock server
        |Testing all day long
      """.trimMargin()
    )
    send(Protocol.NOW_PLAYING_LYRICS, json.encodeToJsonElement(LyricsPayload.serializer(), lyrics))
  }

  private fun sendNowPlayingList(message: SocketMessage) {
    val offset = message.data.jsonObject["offset"]?.jsonPrimitive?.int ?: 0
    val limit = message.data.jsonObject["limit"]?.jsonPrimitive?.int ?: 50

    val tracks = state.playlist.drop(offset).take(limit)
    val paged = PagedData(
      offset = offset,
      limit = limit,
      total = state.playlist.size,
      data = tracks
    )
    send(Protocol.NOW_PLAYING_LIST, json.encodeToJsonElement(PagedData.serializer(
      com.kelsos.mbrc.mock.protocol.NowPlayingTrack.serializer()
    ), paged))
  }

  private fun sendOutputDevices() {
    val devices = listOf(
      OutputDevice("Default Speakers", true),
      OutputDevice("Headphones", false),
      OutputDevice("Bluetooth Speaker", false)
    )
    send(Protocol.PLAYER_OUTPUT, json.encodeToJsonElement(
      kotlinx.serialization.builtins.ListSerializer(OutputDevice.serializer()),
      devices
    ))
  }

  private fun sendLibraryGenres(message: SocketMessage) {
    val offset = message.data.jsonObject["offset"]?.jsonPrimitive?.int ?: 0
    val limit = message.data.jsonObject["limit"]?.jsonPrimitive?.int ?: 50
    val genres = state.library.genres.drop(offset).take(limit)
    val paged = PagedData(offset, limit, state.library.genres.size, genres)
    send(Protocol.LIBRARY_BROWSE_GENRES, json.encodeToJsonElement(PagedData.serializer(
      com.kelsos.mbrc.mock.protocol.GenreDto.serializer()
    ), paged))
  }

  private fun sendLibraryArtists(message: SocketMessage) {
    val offset = message.data.jsonObject["offset"]?.jsonPrimitive?.int ?: 0
    val limit = message.data.jsonObject["limit"]?.jsonPrimitive?.int ?: 50
    val artists = state.library.artists.drop(offset).take(limit)
    val paged = PagedData(offset, limit, state.library.artists.size, artists)
    send(Protocol.LIBRARY_BROWSE_ARTISTS, json.encodeToJsonElement(PagedData.serializer(
      com.kelsos.mbrc.mock.protocol.ArtistDto.serializer()
    ), paged))
  }

  private fun sendLibraryAlbums(message: SocketMessage) {
    val offset = message.data.jsonObject["offset"]?.jsonPrimitive?.int ?: 0
    val limit = message.data.jsonObject["limit"]?.jsonPrimitive?.int ?: 50
    val albums = state.library.albums.drop(offset).take(limit)
    val paged = PagedData(offset, limit, state.library.albums.size, albums)
    send(Protocol.LIBRARY_BROWSE_ALBUMS, json.encodeToJsonElement(PagedData.serializer(
      com.kelsos.mbrc.mock.protocol.AlbumDto.serializer()
    ), paged))
  }

  private fun sendLibraryTracks(message: SocketMessage) {
    val offset = message.data.jsonObject["offset"]?.jsonPrimitive?.int ?: 0
    val limit = message.data.jsonObject["limit"]?.jsonPrimitive?.int ?: 50
    val tracks = state.library.tracks.drop(offset).take(limit)
    val paged = PagedData(offset, limit, state.library.tracks.size, tracks)
    send(Protocol.LIBRARY_BROWSE_TRACKS, json.encodeToJsonElement(PagedData.serializer(
      com.kelsos.mbrc.mock.protocol.TrackDto.serializer()
    ), paged))
  }

  private fun sendRadioStations(message: SocketMessage) {
    val offset = message.data.jsonObject["offset"]?.jsonPrimitive?.int ?: 0
    val limit = message.data.jsonObject["limit"]?.jsonPrimitive?.int ?: 50
    val stations = state.radioStations.stations.drop(offset).take(limit)
    val paged = PagedData(offset, limit, state.radioStations.stations.size, stations)
    send(Protocol.RADIO_STATIONS, json.encodeToJsonElement(PagedData.serializer(
      com.kelsos.mbrc.mock.protocol.RadioStationDto.serializer()
    ), paged))
  }

  private fun sendPlaylists(message: SocketMessage) {
    val offset = message.data.jsonObject["offset"]?.jsonPrimitive?.int ?: 0
    val limit = message.data.jsonObject["limit"]?.jsonPrimitive?.int ?: 50
    val playlists = state.playlists.playlists.drop(offset).take(limit)
    val paged = PagedData(offset, limit, state.playlists.playlists.size, playlists)
    send(Protocol.PLAYLIST_LIST, json.encodeToJsonElement(PagedData.serializer(
      com.kelsos.mbrc.mock.protocol.PlaylistDto.serializer()
    ), paged))
  }
}
