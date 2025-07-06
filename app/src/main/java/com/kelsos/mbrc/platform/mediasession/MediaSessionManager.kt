package com.kelsos.mbrc.platform.mediasession

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.kelsos.mbrc.common.state.AppStateFlow
import com.kelsos.mbrc.common.state.orEmpty
import com.kelsos.mbrc.common.state.toMediaItem
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.networking.protocol.UserActionUseCase
import com.kelsos.mbrc.networking.protocol.VolumeModifyUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

/**
 * Manages the media session for the application.
 */
class MediaSessionManager(
  private val context: Context,
  private val userActionUseCase: UserActionUseCase,
  private val volumeModifyUseCase: VolumeModifyUseCase,
  private val appState: AppStateFlow,
  private val dispatchers: AppCoroutineDispatchers
) {
  private var _mediaSession: MediaSession? = null
  private val sessionJob: Job = Job()
  val scope: CoroutineScope = CoroutineScope(sessionJob)

  /**
   * Initializes the media session if it doesn't already exist.
   *
   * @return The initialized media session.
   */
  @OptIn(UnstableApi::class)
  fun initialize(): MediaSession {
    if (_mediaSession != null) {
      return requireNotNull(_mediaSession)
    }

    val player =
      RemotePlayer(context, userActionUseCase, volumeModifyUseCase, appState, dispatchers, scope)

    val mediaSessionCallback =
      object : MediaSession.Callback {
        override fun onPlaybackResumption(
          mediaSession: MediaSession,
          controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
          val item =
            runBlocking {
              val currentPlayingTrack = appState.playingTrack.firstOrNull()
              currentPlayingTrack
                .orEmpty()
                .toMediaItem()
            }
          return Futures.immediateFuture(
            MediaSession.MediaItemsWithStartPosition(listOf(item), 0, 0)
          )
        }
      }

    _mediaSession =
      MediaSession
        .Builder(context, player)
        .setCallback(mediaSessionCallback)
        .setId(AppNotificationManager.MEDIA_SESSION_NOTIFICATION_ID.toString())
        .build()

    return requireNotNull(_mediaSession)
  }

  /**
   * Destroys the media session if it exists.
   */
  fun destroy() {
    scope.cancel()
    _mediaSession?.run {
      player.release()
      release()
      _mediaSession = null
    }
  }

  /**
   * Gets the current media session.
   *
   * @return The current media session, or null if it hasn't been initialized.
   */
  val mediaSession: MediaSession?
    get() = _mediaSession
}
