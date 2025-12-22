package com.kelsos.mbrc.mock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
  val port = args.getOrNull(0)?.toIntOrNull() ?: 3000

  println("""
    |╔══════════════════════════════════════════════════════════════╗
    |║         MusicBee Remote - Mock Server                        ║
    |╠══════════════════════════════════════════════════════════════╣
    |║  TCP Port: $port
    |║  UDP Discovery: 239.1.5.10:45345                             ║
    |╚══════════════════════════════════════════════════════════════╝
  """.trimMargin())

  val state = MockState()
  val tcpServer = MockServer(port, state)
  val discoveryResponder = DiscoveryResponder(port)

  // Handle shutdown
  Runtime.getRuntime().addShutdownHook(Thread {
    println("\nShutting down...")
    tcpServer.stop()
    discoveryResponder.stop()
  })

  runBlocking {
    // Start UDP discovery in background
    launch(Dispatchers.IO) {
      discoveryResponder.start()
    }

    // Start TCP server (blocking)
    launch(Dispatchers.IO) {
      tcpServer.start()
    }

    // Keep main alive
    println("\nPress Ctrl+C to stop the server\n")

    // Interactive mode
    CoroutineScope(Dispatchers.IO).launch {
      while (true) {
        try {
          val input = readlnOrNull() ?: continue
          handleCommand(input, state)
        } catch (e: Exception) {
          // Ignore
        }
      }
    }.join()
  }
}

private fun handleCommand(input: String, state: MockState) {
  val parts = input.trim().split(" ")
  when (parts.firstOrNull()?.lowercase()) {
    "help", "?" -> printHelp()
    "status" -> printStatus(state)
    "play" -> {
      state.playState = com.kelsos.mbrc.mock.protocol.PlayState.Playing
      println("▶ Playing")
    }
    "pause" -> {
      state.playState = com.kelsos.mbrc.mock.protocol.PlayState.Paused
      println("⏸ Paused")
    }
    "stop" -> {
      state.playState = com.kelsos.mbrc.mock.protocol.PlayState.Stopped
      println("⏹ Stopped")
    }
    "next" -> {
      state.nextTrack()
      println("⏭ Next track: ${state.currentTrack.title}")
    }
    "prev" -> {
      state.previousTrack()
      println("⏮ Previous track: ${state.currentTrack.title}")
    }
    "vol", "volume" -> {
      val vol = parts.getOrNull(1)?.toIntOrNull()
      if (vol != null) {
        state.volume = vol.coerceIn(0, 100)
        println("🔊 Volume: ${state.volume}")
      } else {
        println("Usage: vol <0-100>")
      }
    }
    "track" -> printTrack(state)
    "playlist" -> printPlaylist(state)
    else -> {
      if (input.isNotBlank()) {
        println("Unknown command. Type 'help' for available commands.")
      }
    }
  }
}

private fun printHelp() {
  println("""
    |Available commands:
    |  help, ?     - Show this help
    |  status      - Show current player status
    |  play        - Set play state to playing
    |  pause       - Set play state to paused
    |  stop        - Set play state to stopped
    |  next        - Skip to next track
    |  prev        - Skip to previous track
    |  vol <0-100> - Set volume
    |  track       - Show current track info
    |  playlist    - Show current playlist
  """.trimMargin())
}

private fun printStatus(state: MockState) {
  println("""
    |Player Status:
    |  State:     ${state.playState.value}
    |  Volume:    ${state.volume}%
    |  Mute:      ${state.mute}
    |  Shuffle:   ${state.shuffle.value}
    |  Repeat:    ${state.repeat.value}
    |  Scrobbling:${state.scrobbling}
  """.trimMargin())
}

private fun printTrack(state: MockState) {
  val track = state.currentTrack
  println("""
    |Now Playing:
    |  Title:  ${track.title}
    |  Artist: ${track.artist}
    |  Album:  ${track.album}
    |  Year:   ${track.year}
  """.trimMargin())
}

private fun printPlaylist(state: MockState) {
  println("Playlist (${state.playlist.size} tracks):")
  state.playlist.forEachIndexed { index, track ->
    val marker = if (index == state.currentTrackIndex) "▶" else " "
    println("  $marker ${index + 1}. ${track.artist} - ${track.title}")
  }
}
