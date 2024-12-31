package com.kelsos.mbrc.networking.protocol

sealed class Protocol(
  val context: String,
) {
  companion object {
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
    const val NOW_PLAYING_LIST_SEARCH = "nowplayinglistsearch"
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

    const val VERIFY_CONNECTION = "verifyconnection"
    const val RADIO_STATIONS = "radiostations"

    const val PLAYER_OUTPUT = "playeroutput"
    const val PLAYER_OUTPUT_SWITCH = "playeroutputswitch"

    const val LIBRARY_COVER = "libraryalbumcover"

    const val PROTOCOL_VERSION_NUMBER = 4

    const val ONE = "one"
    const val ALL = "All"
  }
}
