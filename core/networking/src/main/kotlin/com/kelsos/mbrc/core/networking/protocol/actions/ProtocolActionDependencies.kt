package com.kelsos.mbrc.core.networking.protocol.actions

import com.kelsos.mbrc.core.common.state.PlayerStatusModel
import com.kelsos.mbrc.core.common.state.PlayingPosition
import com.kelsos.mbrc.core.common.state.TrackDetails
import com.kelsos.mbrc.core.common.state.TrackInfo
import com.kelsos.mbrc.core.common.state.TrackRating
import kotlinx.coroutines.flow.Flow

/**
 * Interface for publishing and subscribing to player state.
 * Abstracts the app state management for use in protocol actions.
 */
interface PlayerStateHandler {
  val playerStatus: Flow<PlayerStatusModel>
  val playingTrack: Flow<TrackInfo>
  val playingTrackRating: Flow<TrackRating>
  val playingTrackDetails: Flow<TrackDetails>

  fun updatePlayerStatus(status: PlayerStatusModel)
  fun updatePlayingTrack(track: TrackInfo)
  fun updateTrackRating(rating: TrackRating)
  fun updateTrackDetails(details: TrackDetails)
  fun updateLyrics(lyrics: List<String>)
  fun updatePlayingPosition(position: PlayingPosition)
}

/**
 * Interface for notifying external components about track and play state changes.
 * Used for widget updates and track info persistence.
 */
interface TrackChangeNotifier {
  suspend fun persistTrackInfo(track: TrackInfo)
  suspend fun requestTrackDetails()
}

/**
 * Interface for handling cover art retrieval and storage.
 * Abstracts the cover fetching and file storage operations.
 */
interface CoverHandler {
  /**
   * Fetches and stores the cover from the remote API.
   * @return The URI of the stored cover, or empty string if failed.
   */
  suspend fun fetchAndStoreCover(): String

  /**
   * Clears any previously stored covers.
   */
  suspend fun clearCovers()
}

/**
 * Interface for now playing list operations.
 * Abstracts the now playing repository for protocol actions.
 */
interface NowPlayingHandler {
  suspend fun removeTrack(position: Int)
  suspend fun refreshFromRemote()
}

/**
 * Interface for plugin version checking.
 * Handles checking if plugin updates are needed.
 */
interface PluginVersionHandler {
  suspend fun onVersionReceived(version: String)
}
