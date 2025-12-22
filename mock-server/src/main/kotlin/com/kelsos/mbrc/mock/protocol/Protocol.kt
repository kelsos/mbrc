package com.kelsos.mbrc.mock.protocol

object Protocol {
  const val PLAYER = "player"
  const val PROTOCOL_TAG = "protocol"
  const val PLUGIN_VERSION = "pluginversion"
  const val CLIENT_NOT_ALLOWED = "notallowed"

  const val PLAYER_STATUS = "playerstatus"
  const val PLAYER_REPEAT = "playerrepeat"
  const val PLAYER_SCROBBLE = "scrobbler"
  const val PLAYER_SHUFFLE = "playershuffle"
  const val PLAYER_MUTE = "playermute"
  const val PLAYER_PLAY_PAUSE = "playerplaypause"
  const val PLAYER_PREVIOUS = "playerprevious"
  const val PLAYER_NEXT = "playernext"
  const val PLAYER_STOP = "playerstop"
  const val PLAYER_STATE = "playerstate"
  const val PLAYER_VOLUME = "playervolume"

  const val NOW_PLAYING_TRACK = "nowplayingtrack"
  const val NOW_PLAYING_COVER = "nowplayingcover"
  const val NOW_PLAYING_POSITION = "nowplayingposition"
  const val NOW_PLAYING_LYRICS = "nowplayinglyrics"
  const val NOW_PLAYING_RATING = "nowplayingrating"
  const val NOW_PLAYING_LFM_RATING = "nowplayinglfmrating"
  const val NOW_PLAYING_LIST = "nowplayinglist"
  const val NOW_PLAYING_LIST_PLAY = "nowplayinglistplay"
  const val NOW_PLAYING_LIST_REMOVE = "nowplayinglistremove"
  const val NOW_PLAYING_LIST_MOVE = "nowplayinglistmove"
  const val NOW_PLAYING_QUEUE = "nowplayingqueue"

  const val PING = "ping"
  const val PONG = "pong"
  const val INIT = "init"

  const val PLAYER_PLAY = "playerplay"
  const val PLAYER_PAUSE = "playerpause"

  const val PLAYLIST_LIST = "playlistlist"
  const val PLAYLIST_PLAY = "playlistplay"

  const val LIBRARY_BROWSE_GENRES = "browsegenres"
  const val LIBRARY_BROWSE_ARTISTS = "browseartists"
  const val LIBRARY_BROWSE_ALBUMS = "browsealbums"
  const val LIBRARY_BROWSE_TRACKS = "browsetracks"

  const val DISCOVERY = "discovery"
  const val NOTIFY = "notify"

  const val VERIFY_CONNECTION = "verifyconnection"
  const val RADIO_STATIONS = "radiostations"

  const val COMMAND_UNAVAILABLE = "commandunavailable"

  const val PLAYER_OUTPUT = "playeroutput"
  const val PLAYER_OUTPUT_SWITCH = "playeroutputswitch"

  const val LIBRARY_COVER = "libraryalbumcover"

  const val TOGGLE = "toggle"
  const val PROTOCOL_VERSION = 4

  // Discovery
  const val DISCOVERY_ADDRESS = "239.1.5.10"
  const val DISCOVERY_PORT = 45345
}

enum class PlayState(val value: String) {
  Playing("playing"),
  Paused("paused"),
  Stopped("stopped");

  companion object {
    fun toggle(current: PlayState): PlayState = when (current) {
      Playing -> Paused
      Paused -> Playing
      Stopped -> Playing
    }
  }
}

enum class RepeatMode(val value: String) {
  None("none"),
  One("one"),
  All("all");

  companion object {
    fun toggle(current: RepeatMode): RepeatMode = when (current) {
      None -> All
      All -> One
      One -> None
    }
  }
}

enum class ShuffleMode(val value: String) {
  Off("off"),
  Shuffle("shuffle"),
  AutoDj("autodj");

  companion object {
    fun toggle(current: ShuffleMode): ShuffleMode = when (current) {
      Off -> Shuffle
      Shuffle -> AutoDj
      AutoDj -> Off
    }
  }
}
