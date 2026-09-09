package com.kelsos.mbrc.adapters

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.core.net.toUri
import com.kelsos.mbrc.core.common.state.AppStatePublisher
import com.kelsos.mbrc.core.common.state.PlayerStatusModel
import com.kelsos.mbrc.core.common.state.PlayingPosition
import com.kelsos.mbrc.core.common.state.TrackDetails
import com.kelsos.mbrc.core.common.state.TrackInfo
import com.kelsos.mbrc.core.common.state.TrackRating
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.networking.api.PlaybackApi
import com.kelsos.mbrc.core.networking.protocol.SelfMutationTracker
import com.kelsos.mbrc.core.networking.protocol.actions.CoverHandler
import com.kelsos.mbrc.core.networking.protocol.actions.NowPlayingHandler
import com.kelsos.mbrc.core.networking.protocol.actions.PlayerStateHandler
import com.kelsos.mbrc.core.networking.protocol.actions.PluginVersionHandler
import com.kelsos.mbrc.core.networking.protocol.actions.TrackChangeNotifier
import com.kelsos.mbrc.core.platform.state.toPlayingTrack
import com.kelsos.mbrc.feature.playback.nowplaying.NowPlayingRepository
import com.kelsos.mbrc.feature.settings.domain.PluginUpdateCheckUseCase
import com.kelsos.mbrc.state.PlayingTrackCache
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okio.HashingSink
import okio.blackholeSink
import okio.buffer
import okio.source
import timber.log.Timber

/**
 * Adapts [AppStatePublisher] to [PlayerStateHandler] interface.
 */
class PlayerStateHandlerImpl(private val appState: AppStatePublisher) : PlayerStateHandler {
  override val playerStatus: Flow<PlayerStatusModel> = appState.playerStatus
  override val playingTrack: Flow<TrackInfo> = appState.playingTrack
  override val playingTrackRating: Flow<TrackRating> = appState.playingTrackRating
  override val playingTrackDetails: Flow<TrackDetails> = appState.playingTrackDetails

  override fun updatePlayerStatus(status: PlayerStatusModel) {
    appState.updatePlayerStatus(status)
  }

  override fun updatePlayingTrack(track: TrackInfo) {
    appState.updatePlayingTrack(track.toPlayingTrack())
  }

  override fun updateTrackRating(rating: TrackRating) {
    appState.updateTrackRating(rating)
  }

  override fun updateTrackDetails(details: TrackDetails) {
    appState.updateTrackDetails(details)
  }

  override fun updateLyrics(lyrics: List<String>) {
    appState.updateLyrics(lyrics)
  }

  override fun updatePlayingPosition(position: PlayingPosition) {
    appState.updatePlayingPosition(position)
  }
}

/**
 * Adapts [PlayingTrackCache] to [TrackChangeNotifier] interface.
 */
class TrackChangeNotifierImpl(
  private val cache: PlayingTrackCache,
  private val playbackApi: PlaybackApi,
  private val appState: AppStatePublisher,
  private val dispatchers: AppCoroutineDispatchers
) : TrackChangeNotifier {
  override suspend fun persistTrackInfo(track: TrackInfo) {
    cache.persistInfo(track.toPlayingTrack())
  }

  override suspend fun requestTrackDetails() {
    withContext(dispatchers.network) {
      runCatching {
        val details = playbackApi.getTrackDetails()
        appState.updateTrackDetails(details.toTrackDetails())
      }.onFailure { e ->
        Timber.v(e, "Failed to fetch track details")
      }
    }
  }
}

/**
 * Adapts [NowPlayingRepository] to [NowPlayingHandler] interface.
 */
class NowPlayingHandlerImpl(
  private val repository: NowPlayingRepository,
  private val selfMutationTracker: SelfMutationTracker
) : NowPlayingHandler {
  override suspend fun removeTrack(position: Int) {
    repository.remove(position)
  }

  override suspend fun refreshFromRemote() {
    if (selfMutationTracker.wasRecentlyMarked()) {
      Timber.v("skipping refresh — recent self-initiated mutation")
      return
    }
    runCatching {
      repository.getRemote()
    }.onFailure { e ->
      Timber.v(e, "Failed to refresh now playing list from remote")
    }
  }
}

/**
 * Adapts [PluginUpdateCheckUseCase] to [PluginVersionHandler] interface.
 */
class PluginVersionHandlerImpl(private val pluginUpdateCheck: PluginUpdateCheckUseCase) :
  PluginVersionHandler {
  override suspend fun onVersionReceived(version: String) {
    pluginUpdateCheck.checkIfUpdateNeeded(version)
  }
}

/**
 * Implementation of [CoverHandler] that fetches and stores cover art.
 */
class CoverHandlerImpl(
  private val app: Application,
  private val playbackApi: PlaybackApi,
  private val dispatchers: AppCoroutineDispatchers
) : CoverHandler {
  private val coverDir: File = File(app.filesDir, COVER_DIR)

  override suspend fun fetchAndStoreCover(): String = withContext(dispatchers.network) {
    val result = runCatching {
      val response = playbackApi.getCover()
      val cover = checkNotNull(response.cover) { "Cover payload had no cover data" }
      val bitmap = getBitmap(cover)
      val file = storeCover(bitmap)
      file.toUri().toString()
    }

    if (result.isFailure) {
      Timber.v(result.exceptionOrNull(), "Failed to store cover")
      clearPreviousCovers(0)
      ""
    } else {
      result.getOrDefault("")
    }
  }

  override suspend fun clearCovers() {
    withContext(dispatchers.network) {
      clearPreviousCovers(0)
    }
  }

  private fun getBitmap(base64: String): Bitmap {
    val decodedImage = Base64.decode(base64, Base64.DEFAULT)
    val bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size)
    return checkNotNull(bitmap) { "Base64 was not an image" }
  }

  private fun storeCover(bitmap: Bitmap): File {
    checkIfExists()
    clearPreviousCovers()

    val file = temporaryCover()
    val fileStream = FileOutputStream(file)
    val success = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fileStream)
    fileStream.close()

    val md5 =
      HashingSink.md5(blackholeSink()).use { hashingSink ->
        file.source().buffer().use { source ->
          source.readAll(hashingSink)
          hashingSink.hash.md5().hex()
        }
      }

    val extension = file.extension
    val newFile = File(app.filesDir, "$md5.$extension")
    if (newFile.exists()) {
      val isDeleted = file.delete()
      if (!isDeleted) {
        Timber.v("unable to delete temporary cover ${file.absolutePath}")
      }
      return newFile
    }

    check(success) { "unable to store cover" }
    val isRenamed = file.renameTo(newFile)
    Timber.v("file was renamed $isRenamed to ${newFile.absolutePath}")
    return newFile
  }

  private fun checkIfExists() {
    if (!coverDir.exists()) {
      val isSuccessful = coverDir.mkdir()
      Timber.v("cover directory ${coverDir.absolutePath} creation is successful $isSuccessful")
    }
  }

  private fun clearPreviousCovers(keep: Int = 1) {
    if (!coverDir.exists()) {
      return
    }
    val storedCovers = coverDir.listFiles() ?: return
    storedCovers.sortByDescending(File::lastModified)
    val elementsToKeep = if (storedCovers.size - keep < 0) 0 else storedCovers.size - keep
    storedCovers.takeLast(elementsToKeep).forEach {
      val isSuccessful = it.delete()
      if (!isSuccessful) {
        Timber.v("unable to cover delete ${it.absolutePath}")
      }
    }
  }

  private fun temporaryCover(): File {
    val file = File(app.cacheDir, TEMP_COVER)
    if (file.exists()) {
      val isSuccessful = file.delete()
      if (!isSuccessful) {
        Timber.v("unable to delete temporary cover ${file.absolutePath}")
      }
    }
    return file
  }

  companion object {
    const val COVER_DIR = "cover"
    const val TEMP_COVER = "temp_cover.jpg"
    const val JPEG_QUALITY = 100
  }
}
