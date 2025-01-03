package com.kelsos.mbrc.common.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AppStateFlow {
  val playerStatus: StateFlow<PlayerStatusModel>
  val playingTrack: StateFlow<PlayingTrack>
  val playingTrackRating: StateFlow<TrackRating>
  val playingPosition: StateFlow<PlayingPosition>
  val lyrics: StateFlow<List<String>>
}

interface AppStatePublisher : AppStateFlow {
  suspend fun updatePlayerStatus(status: PlayerStatusModel)

  suspend fun updatePlayingTrack(track: PlayingTrack)

  suspend fun updateTrackRating(rating: TrackRating)

  suspend fun updatePlayingPosition(position: PlayingPosition)

  suspend fun updateLyrics(lyrics: List<String>)
}

class AppState : AppStatePublisher {
  private val _playerStatus = MutableStateFlow(PlayerStatusModel())
  private val _playingTrack = MutableStateFlow(PlayingTrack())
  private val _playingTrackRating = MutableStateFlow(TrackRating())
  private val _playingPosition = MutableStateFlow(PlayingPosition())
  private val _lyrics = MutableStateFlow(emptyList<String>())

  override val playerStatus: StateFlow<PlayerStatusModel>
    get() = _playerStatus
  override val playingTrack: StateFlow<PlayingTrack>
    get() = _playingTrack
  override val playingTrackRating: StateFlow<TrackRating>
    get() = _playingTrackRating
  override val playingPosition: StateFlow<PlayingPosition>
    get() = _playingPosition
  override val lyrics: StateFlow<List<String>>
    get() = _lyrics

  override suspend fun updatePlayerStatus(status: PlayerStatusModel) {
    _playerStatus.emit(status)
  }

  override suspend fun updatePlayingTrack(track: PlayingTrack) {
    _playingTrack.emit(track)
  }

  override suspend fun updateTrackRating(rating: TrackRating) {
    _playingTrackRating.emit(rating)
  }

  override suspend fun updatePlayingPosition(position: PlayingPosition) {
    _playingPosition.emit(position)
  }

  override suspend fun updateLyrics(lyrics: List<String>) {
    _lyrics.emit(lyrics)
  }
}
