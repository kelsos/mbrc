package com.kelsos.mbrc.feature.playback.player

import com.kelsos.mbrc.core.common.state.LfmRating
import com.kelsos.mbrc.core.networking.protocol.actions.UserAction
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import com.kelsos.mbrc.core.networking.protocol.usecases.UserActionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

interface IPlayerActions {
  val playPause: () -> Unit
  val previous: () -> Unit
  val next: () -> Unit
  val stop: () -> Unit
  val shuffle: () -> Unit
  val repeat: () -> Unit
  val mute: () -> Unit
  val changeVolume: (Int) -> Unit
  val seek: (Int) -> Unit
  val toggleFavorite: (isFavorite: Boolean, isBanned: Boolean) -> Unit
  val toggleBan: (isBanned: Boolean, isFavorite: Boolean) -> Unit
  val toggleScrobbling: () -> Unit
}

class PlayerActions(
  private val userActionUseCase: UserActionUseCase,
  private val scope: CoroutineScope,
  private val progressRelay: MutableSharedFlow<Int>,
  private val volumeRelay: MutableSharedFlow<Int>
) : IPlayerActions {

  override val playPause: () -> Unit = {
    scope.launch {
      userActionUseCase.perform(UserAction(Protocol.PlayerPlayPause, true))
    }
  }

  override val previous: () -> Unit = {
    scope.launch {
      userActionUseCase.perform(UserAction(Protocol.PlayerPrevious, true))
    }
  }

  override val next: () -> Unit = {
    scope.launch {
      userActionUseCase.perform(UserAction(Protocol.PlayerNext, true))
    }
  }

  override val stop: () -> Unit = {
    scope.launch {
      userActionUseCase.perform(UserAction(Protocol.PlayerStop, true))
    }
  }

  override val shuffle: () -> Unit = {
    scope.launch {
      userActionUseCase.perform(UserAction.toggle(Protocol.PlayerShuffle))
    }
  }

  override val repeat: () -> Unit = {
    scope.launch {
      userActionUseCase.perform(UserAction.toggle(Protocol.PlayerRepeat))
    }
  }

  override val mute: () -> Unit = {
    scope.launch {
      userActionUseCase.perform(UserAction.toggle(Protocol.PlayerMute))
    }
  }

  override val changeVolume: (Int) -> Unit = { volume ->
    scope.launch {
      volumeRelay.emit(volume)
    }
  }

  override val seek: (Int) -> Unit = { position ->
    scope.launch {
      progressRelay.emit(position)
    }
  }

  override val toggleFavorite: (Boolean, Boolean) -> Unit = { isFavorite, isBanned ->
    scope.launch {
      Timber.d("toggleFavorite: isFavorite=$isFavorite, isBanned=$isBanned")
      when {
        isFavorite -> {
          // Currently Loved, toggle to Normal
          userActionUseCase.perform(
            UserAction.create(Protocol.NowPlayingLfmRating, "toggle")
          )
        }

        isBanned -> {
          // Currently Banned, switch to Love
          // Workaround: Plugin has race condition when going Ban->Love->toggle
          // So we go Ban->Normal->Love to ensure clean state
          userActionUseCase.perform(
            UserAction.create(Protocol.NowPlayingLfmRating, "toggle")
          )
          userActionUseCase.perform(
            UserAction.create(Protocol.NowPlayingLfmRating, LfmRating.Loved.toActionString())
          )
        }

        else -> {
          // Currently Normal, set to Love
          userActionUseCase.perform(
            UserAction.create(Protocol.NowPlayingLfmRating, LfmRating.Loved.toActionString())
          )
        }
      }
    }
  }

  override val toggleBan: (Boolean, Boolean) -> Unit = { isBanned, isFavorite ->
    scope.launch {
      Timber.d("toggleBan: isBanned=$isBanned, isFavorite=$isFavorite")
      when {
        isBanned -> {
          // Currently Banned, toggle to Normal
          userActionUseCase.perform(
            UserAction.create(Protocol.NowPlayingLfmRating, "toggle")
          )
        }

        isFavorite -> {
          // Currently Loved, switch to Ban
          // Workaround: Plugin has race condition when going Love->Ban->toggle
          // So we go Love->Normal->Ban to ensure clean state
          userActionUseCase.perform(
            UserAction.create(Protocol.NowPlayingLfmRating, "toggle")
          )
          userActionUseCase.perform(
            UserAction.create(Protocol.NowPlayingLfmRating, LfmRating.Banned.toActionString())
          )
        }

        else -> {
          // Currently Normal, set to Ban
          userActionUseCase.perform(
            UserAction.create(Protocol.NowPlayingLfmRating, LfmRating.Banned.toActionString())
          )
        }
      }
    }
  }

  override val toggleScrobbling: () -> Unit = {
    scope.launch {
      userActionUseCase.perform(UserAction.toggle(Protocol.PlayerScrobble))
    }
  }
}
