package com.kelsos.mbrc.mock

import com.kelsos.mbrc.mock.protocol.AlbumDto
import com.kelsos.mbrc.mock.protocol.ArtistDto
import com.kelsos.mbrc.mock.protocol.GenreDto
import com.kelsos.mbrc.mock.protocol.NowPlayingTrack
import com.kelsos.mbrc.mock.protocol.PlayState
import com.kelsos.mbrc.mock.protocol.PlaylistDto
import com.kelsos.mbrc.mock.protocol.RadioStationDto
import com.kelsos.mbrc.mock.protocol.RepeatMode
import com.kelsos.mbrc.mock.protocol.ShuffleMode
import com.kelsos.mbrc.mock.protocol.TrackDto

class MockState {
  var playState = PlayState.Paused
  var volume = 75
  var mute = false
  var shuffle = ShuffleMode.Off
  var repeat = RepeatMode.None
  var scrobbling = false
  var position = 0L
  var rating = 0f
  var lfmRating = ""

  var currentTrackIndex = 0

  val playlist: List<NowPlayingTrack> = listOf(
    NowPlayingTrack(
      artist = "Daft Punk",
      title = "Around the World",
      album = "Homework",
      year = "1997",
      path = "/music/daft_punk/homework/around_the_world.mp3"
    ),
    NowPlayingTrack(
      artist = "Daft Punk",
      title = "One More Time",
      album = "Discovery",
      year = "2001",
      path = "/music/daft_punk/discovery/one_more_time.mp3"
    ),
    NowPlayingTrack(
      artist = "Justice",
      title = "D.A.N.C.E.",
      album = "Cross",
      year = "2007",
      path = "/music/justice/cross/dance.mp3"
    ),
    NowPlayingTrack(
      artist = "The Chemical Brothers",
      title = "Block Rockin' Beats",
      album = "Dig Your Own Hole",
      year = "1997",
      path = "/music/chemical_brothers/dig_your_own_hole/block_rockin_beats.mp3"
    ),
    NowPlayingTrack(
      artist = "Fatboy Slim",
      title = "Right Here, Right Now",
      album = "You've Come a Long Way, Baby",
      year = "1998",
      path = "/music/fatboy_slim/youve_come_a_long_way_baby/right_here_right_now.mp3"
    )
  )

  val currentTrack: NowPlayingTrack
    get() = playlist.getOrElse(currentTrackIndex) { playlist.first() }

  val trackDuration: Long = 240000L // 4 minutes in ms

  fun nextTrack() {
    currentTrackIndex = (currentTrackIndex + 1) % playlist.size
    position = 0
  }

  fun previousTrack() {
    currentTrackIndex = if (currentTrackIndex > 0) currentTrackIndex - 1 else playlist.size - 1
    position = 0
  }

  val library = MockLibrary()
  val radioStations = MockRadioStations()
  val playlists = MockPlaylists()
}

class MockLibrary {
  val genres = listOf(
    GenreDto("Electronic"),
    GenreDto("Rock"),
    GenreDto("Jazz"),
    GenreDto("Classical"),
    GenreDto("Hip Hop"),
    GenreDto("Pop"),
    GenreDto("Metal"),
    GenreDto("Blues")
  )

  val artists = listOf(
    ArtistDto("Daft Punk"),
    ArtistDto("Justice"),
    ArtistDto("The Chemical Brothers"),
    ArtistDto("Fatboy Slim"),
    ArtistDto("Moby"),
    ArtistDto("Deadmau5"),
    ArtistDto("Aphex Twin"),
    ArtistDto("Boards of Canada")
  )

  val albums = listOf(
    AlbumDto("Daft Punk", "Homework"),
    AlbumDto("Daft Punk", "Discovery"),
    AlbumDto("Daft Punk", "Random Access Memories"),
    AlbumDto("Justice", "Cross"),
    AlbumDto("Justice", "Audio, Video, Disco"),
    AlbumDto("The Chemical Brothers", "Dig Your Own Hole"),
    AlbumDto("The Chemical Brothers", "Surrender"),
    AlbumDto("Fatboy Slim", "You've Come a Long Way, Baby")
  )

  val tracks = listOf(
    TrackDto(
      artist = "Daft Punk",
      title = "Around the World",
      src = "/music/daft_punk/homework/around_the_world.mp3",
      trackno = 1,
      disc = 1,
      albumArtist = "Daft Punk",
      album = "Homework",
      genre = "Electronic",
      year = "1997"
    ),
    TrackDto(
      artist = "Daft Punk",
      title = "Da Funk",
      src = "/music/daft_punk/homework/da_funk.mp3",
      trackno = 2,
      disc = 1,
      albumArtist = "Daft Punk",
      album = "Homework",
      genre = "Electronic",
      year = "1997"
    ),
    TrackDto(
      artist = "Daft Punk",
      title = "One More Time",
      src = "/music/daft_punk/discovery/one_more_time.mp3",
      trackno = 1,
      disc = 1,
      albumArtist = "Daft Punk",
      album = "Discovery",
      genre = "Electronic",
      year = "2001"
    ),
    TrackDto(
      artist = "Daft Punk",
      title = "Digital Love",
      src = "/music/daft_punk/discovery/digital_love.mp3",
      trackno = 2,
      disc = 1,
      albumArtist = "Daft Punk",
      album = "Discovery",
      genre = "Electronic",
      year = "2001"
    ),
    TrackDto(
      artist = "Justice",
      title = "Genesis",
      src = "/music/justice/cross/genesis.mp3",
      trackno = 1,
      disc = 1,
      albumArtist = "Justice",
      album = "Cross",
      genre = "Electronic",
      year = "2007"
    ),
    TrackDto(
      artist = "Justice",
      title = "D.A.N.C.E.",
      src = "/music/justice/cross/dance.mp3",
      trackno = 2,
      disc = 1,
      albumArtist = "Justice",
      album = "Cross",
      genre = "Electronic",
      year = "2007"
    )
  )
}

class MockRadioStations {
  val stations = listOf(
    RadioStationDto("SomaFM Groove Salad", "http://somafm.com/groovesalad.pls"),
    RadioStationDto("SomaFM Drone Zone", "http://somafm.com/dronezone.pls"),
    RadioStationDto("BBC Radio 1", "http://bbcmedia.ic.llnwd.net/stream/bbcmedia_radio1_mf_p"),
    RadioStationDto("KEXP 90.3", "http://kexp-mp3-128.streamguys1.com/kexp128.mp3"),
    RadioStationDto("FIP Radio", "http://direct.fipradio.fr/live/fip-midfi.mp3")
  )
}

class MockPlaylists {
  val playlists = listOf(
    PlaylistDto("Favorites", "/playlists/favorites.m3u"),
    PlaylistDto("Workout Mix", "/playlists/workout.m3u"),
    PlaylistDto("Chill Vibes", "/playlists/chill.m3u"),
    PlaylistDto("Party Time", "/playlists/party.m3u")
  )
}
