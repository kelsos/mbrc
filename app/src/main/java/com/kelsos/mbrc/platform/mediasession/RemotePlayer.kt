package com.kelsos.mbrc.platform.mediasession

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures.immediateVoidFuture
import com.google.common.util.concurrent.ListenableFuture
import com.kelsos.mbrc.common.state.AppStateFlow
import com.kelsos.mbrc.common.state.PlayerState
import com.kelsos.mbrc.common.state.PlayerStatusModel
import com.kelsos.mbrc.common.state.ShuffleMode
import com.kelsos.mbrc.common.state.orEmpty
import com.kelsos.mbrc.common.state.toMediaItem
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.UserAction
import com.kelsos.mbrc.networking.protocol.UserActionUseCase
import com.kelsos.mbrc.networking.protocol.VolumeModifyUseCase
import com.kelsos.mbrc.networking.protocol.next
import com.kelsos.mbrc.networking.protocol.pause
import com.kelsos.mbrc.networking.protocol.performUserAction
import com.kelsos.mbrc.networking.protocol.play
import com.kelsos.mbrc.networking.protocol.previous
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

@OptIn(UnstableApi::class)
class RemotePlayer(
  context: Context,
  private val userActionUseCase: UserActionUseCase,
  private val volumeModifyUseCase: VolumeModifyUseCase,
  private val appState: AppStateFlow,
  private val dispatchers: AppCoroutineDispatchers,
  scope: CoroutineScope
) : SimpleBasePlayer(context.mainLooper) {
  init {
    appState.playerStatus.invalidateStateOnEach(scope)
    appState.playingPosition.invalidateStateOnEach(scope)
    appState.playingTrack.invalidateStateOnEach(scope)
  }

  private fun <T> StateFlow<T>.invalidateStateOnEach(scope: CoroutineScope) = onEach {
    withContext(dispatchers.main) { invalidateState() }
  }.launchIn(scope)

  private fun getPlaybackState(state: PlayerState): Int = when (state) {
    PlayerState.Playing -> STATE_READY
    PlayerState.Paused -> STATE_READY
    PlayerState.Undefined -> STATE_ENDED
    else -> STATE_IDLE
  }

  override fun getState(): State {
    val commands =
      Player.Commands
        .Builder()
        .add(COMMAND_PLAY_PAUSE)
        .add(COMMAND_STOP)
        .add(COMMAND_PREPARE)
        .add(COMMAND_SET_MEDIA_ITEM)
        .add(COMMAND_GET_CURRENT_MEDIA_ITEM)
        .add(COMMAND_RELEASE)
        .add(COMMAND_SET_SHUFFLE_MODE)
        .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        .add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        .add(COMMAND_SEEK_TO_NEXT)
        .add(COMMAND_SEEK_TO_PREVIOUS)
        .add(COMMAND_SEEK_BACK)
        .add(COMMAND_SEEK_FORWARD)
        .add(COMMAND_GET_CURRENT_MEDIA_ITEM)
        .add(COMMAND_GET_METADATA)
        .add(COMMAND_GET_TIMELINE)
        .add(COMMAND_SEEK_TO_DEFAULT_POSITION)
        .add(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
        .add(COMMAND_GET_DEVICE_VOLUME)
        .add(COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS)
        .add(COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS)
        .build()

    return runBlocking {
      val statusModel = appState.playerStatus.firstOrNull() ?: PlayerStatusModel()
      val position = appState.playingPosition.firstOrNull().orEmpty()
      val playingTrack = appState.playingTrack.firstOrNull().orEmpty()

      val item = playingTrack.toMediaItem()
      val mediaItem =
        MediaItemData
          .Builder(0)
          .setMediaItem(item)
          .setMediaMetadata(item.mediaMetadata)
          .setIsSeekable(true)
          .setDurationUs(
            playingTrack.duration.toDuration(DurationUnit.MILLISECONDS).inWholeMicroseconds
          )
          .build()

      val previous = MediaItemData.Builder("previous-track").build()
      val next = MediaItemData.Builder("next-track").build()

      val playlist = listOf(previous, mediaItem, next)

      State
        .Builder()
        .setAvailableCommands(commands)
        .setAudioAttributes(AudioAttributes.DEFAULT)
        .setPlaybackState(getPlaybackState(statusModel.state))
        .setShuffleModeEnabled(statusModel.shuffle === ShuffleMode.Shuffle)
        .setPlayWhenReady(
          statusModel.state == PlayerState.Playing,
          PLAY_WHEN_READY_CHANGE_REASON_REMOTE
        )
        .setPlaylist(playlist)
        .setPlaylistMetadata(
          MediaMetadata
            .Builder()
            .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
            .setTitle("Now Playing")
            .build()
        ).setCurrentMediaItemIndex(1)
        .setContentPositionMs(position.current)
        .setIsDeviceMuted(statusModel.mute)
        .setDeviceVolume(statusModel.volume)
        .setDeviceInfo(
          DeviceInfo
            .Builder(DeviceInfo.PLAYBACK_TYPE_REMOTE)
            .setMinVolume(MIN_VOLUME)
            .setMaxVolume(MAX_VOLUME)
            .build()
        ).build()
    }
  }

  override fun getPlaceholderMediaItemData(mediaItem: MediaItem): MediaItemData {
    val metadata =
      MediaMetadata
        .Builder()
        .setDisplayTitle("2")
        .setSubtitle("2")
        .setDescription("2")
        .build()
    return MediaItemData
      .Builder(0)
      .setMediaItem(mediaItem)
      .setMediaMetadata(metadata)
      .build()
  }

  override fun handlePrepare(): ListenableFuture<*> = immediateVoidFuture()

  override fun handleRelease(): ListenableFuture<*> = immediateVoidFuture()

  @SuppressLint("SwitchIntDef")
  override fun handleSeek(
    mediaItemIndex: Int,
    positionMs: Long,
    seekCommand: Int
  ): ListenableFuture<*> {
    Timber.d("received seek command: $seekCommand item: $mediaItemIndex at $positionMs")
    runBlocking {
      when (seekCommand) {
        COMMAND_SEEK_TO_PREVIOUS,
        COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
        -> userActionUseCase.previous()

        COMMAND_SEEK_TO_NEXT,
        COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
        -> userActionUseCase.next()
      }

      val to =
        when (positionMs) {
          C.TIME_UNSET -> 0L
          else -> positionMs
        }

      userActionUseCase.performUserAction(Protocol.NowPlayingPosition, to)
    }
    return immediateVoidFuture()
  }

  override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
    Timber.d("received play when ready: $playWhenReady")
    runBlocking {
      if (playWhenReady) {
        userActionUseCase.play()
      } else {
        userActionUseCase.pause()
      }
    }
    return immediateVoidFuture()
  }

  override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
    Timber.d("received shuffle mode enabled: $shuffleModeEnabled")
    runBlocking {
      val mode =
        if (shuffleModeEnabled) {
          ShuffleMode.OFF
        } else {
          ShuffleMode.SHUFFLE
        }
      userActionUseCase.perform(UserAction(Protocol.PlayerShuffle, mode))
    }
    return immediateVoidFuture()
  }

  override fun handleStop(): ListenableFuture<*> {
    runBlocking {
      userActionUseCase.perform(UserAction(Protocol.PlayerStop, true))
    }
    return immediateVoidFuture()
  }

  override fun handleSetDeviceVolume(deviceVolume: Int, flags: Int): ListenableFuture<*> {
    Timber.d("received device volume: $deviceVolume")
    runBlocking {
      userActionUseCase.perform(UserAction(Protocol.PlayerVolume, deviceVolume))
    }
    return immediateVoidFuture()
  }

  override fun handleIncreaseDeviceVolume(flags: Int): ListenableFuture<*> {
    Timber.d("received increase device volume")
    runBlocking {
      volumeModifyUseCase.increase()
    }
    return immediateVoidFuture()
  }

  override fun handleDecreaseDeviceVolume(flags: Int): ListenableFuture<*> {
    Timber.d("received decrease device volume")
    runBlocking {
      volumeModifyUseCase.decrease()
    }
    return immediateVoidFuture()
  }

  override fun handleSetDeviceMuted(muted: Boolean, flags: Int): ListenableFuture<*> {
    Timber.d("received device muted: $muted")
    runBlocking {
      userActionUseCase.perform(UserAction(Protocol.PlayerMute, muted))
    }
    return immediateVoidFuture()
  }

  override fun handleSetMediaItems(
    mediaItems: List<MediaItem>,
    startIndex: Int,
    startPositionMs: Long
  ): ListenableFuture<*> {
    Timber.d("received media items: $mediaItems")
    return immediateVoidFuture()
  }

  companion object {
    private const val MIN_VOLUME = 0
    private const val MAX_VOLUME = 100
  }
}
