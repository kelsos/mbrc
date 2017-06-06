package com.kelsos.mbrc.networking.protocol

import android.support.annotation.StringDef

object Protocol {
  const val Player = "player"
  const val ProtocolTag = "protocol"
  const val PluginVersion = "pluginversion"
  const val ClientNotAllowed = "notallowed"

  const val PlayerStatus = "playerstatus"
  const val PlayerRepeat = "playerrepeat"
  const val PlayerScrobble = "scrobbler"
  const val PlayerShuffle = "playershuffle"
  const val PlayerMute = "playermute"
  const val PlayerPlayPause = "playerplaypause"
  const val PlayerPrevious = "playerprevious"
  const val PlayerNext = "playernext"
  const val PlayerStop = "playerstop"
  const val PlayerState = "playerstate"
  const val PlayerVolume = "playervolume"

  const val NowPlayingTrack = "nowplayingtrack"
  const val NowPlayingCover = "nowplayingcover"
  const val NowPlayingPosition = "nowplayingposition"
  const val NowPlayingLyrics = "nowplayinglyrics"
  const val NowPlayingRating = "nowplayingrating"
  const val NowPlayingLfmRating = "nowplayinglfmrating"
  const val NowPlayingList = "nowplayinglist"
  const val NowPlayingListPlay = "nowplayinglistplay"
  const val NowPlayingListRemove = "nowplayinglistremove"
  const val NowPlayingListMove = "nowplayinglistmove"
  const val NowPlayingListSearch = "nowplayinglistsearch"
  const val NowPlayingQueue = "nowplayingqueue"

  const val PING = "ping"
  const val PONG = "pong"
  const val INIT = "init"

  const val PlayerPlay = "playerplay"
  const val PlayerPause = "playerpause"

  const val PlaylistList = "playlistlist"
  const val PlaylistPlay = "playlistplay"
  const val NoBroadcast = "nobroadcast"

  const val LibraryBrowseGenres = "browsegenres"
  const val LibraryBrowseArtists = "browseartists"
  const val LibraryBrowseAlbums = "browsealbums"
  const val LibraryBrowseTracks = "browsetracks"

  const val DISCOVERY = "discovery"

  const val VerifyConnection = "verifyconnection"
  const val RadioStations = "radiostations"

  const val CommandUnavailable = "commandunavailable"

  // Protocol Constants
  const val CLIENT_PLATFORM = "Android"

  // Repeat Constants
  const val ONE = "one"
  const val ALL = "All"


  /**
   * Toggle action in protocol. This should be send to the functions with multiple states
   * in order to change to the next in order state.
   */
  const val TOGGLE = "toggle"

  const val ProtocolVersionNumber = 5

  @StringDef()
  @Retention(AnnotationRetention.SOURCE)
  annotation class Context
}
