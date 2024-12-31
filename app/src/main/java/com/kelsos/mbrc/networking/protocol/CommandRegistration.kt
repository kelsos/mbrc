package com.kelsos.mbrc.networking.protocol

import com.kelsos.mbrc.commands.CancelNotificationCommand
import com.kelsos.mbrc.commands.ConnectionStatusChangedCommand
import com.kelsos.mbrc.commands.HandleHandshake
import com.kelsos.mbrc.commands.InitiateConnectionCommand
import com.kelsos.mbrc.commands.KeyVolumeDownCommand
import com.kelsos.mbrc.commands.KeyVolumeUpCommand
import com.kelsos.mbrc.commands.ProcessUserAction
import com.kelsos.mbrc.commands.ProtocolRequest
import com.kelsos.mbrc.commands.ReduceVolumeOnRingCommand
import com.kelsos.mbrc.commands.RestartConnectionCommand
import com.kelsos.mbrc.commands.SocketDataAvailableCommand
import com.kelsos.mbrc.commands.StartDiscoveryCommand
import com.kelsos.mbrc.commands.TerminateConnectionCommand
import com.kelsos.mbrc.commands.TerminateServiceCommand
import com.kelsos.mbrc.commands.VersionCheckCommand
import com.kelsos.mbrc.commands.visual.HandshakeCompletionActions
import com.kelsos.mbrc.commands.visual.NotifyNotAllowedCommand
import com.kelsos.mbrc.constants.ApplicationEvents
import com.kelsos.mbrc.constants.ProtocolEventType
import com.kelsos.mbrc.constants.UserInputEventType
import toothpick.Scope

object CommandRegistration {
  fun register(
    controller: RemoteController,
    scope: Scope,
  ) {
    controller.register(
      ProtocolEventType.REDUCE_VOLUME,
      scope.getInstance(ReduceVolumeOnRingCommand::class.java),
    )
    controller.register(
      ProtocolEventType.HANDSHAKE_COMPLETE,
      scope.getInstance(HandshakeCompletionActions::class.java),
    )
    controller.register(
      ProtocolEventType.INFORM_CLIENT_NOT_ALLOWED,
      scope.getInstance(NotifyNotAllowedCommand::class.java),
    )
    controller.register(
      ProtocolEventType.INITIATE_PROTOCOL_REQUEST,
      scope.getInstance(ProtocolRequest::class.java),
    )
    controller.register(
      ProtocolEventType.PLUGIN_VERSION_CHECK,
      scope.getInstance(VersionCheckCommand::class.java),
    )
    controller.register(
      ProtocolEventType.USER_ACTION,
      scope.getInstance(ProcessUserAction::class.java),
    )

    controller.register(
      Protocol.NOW_PLAYING_TRACK,
      scope.getInstance(UpdateNowPlayingTrack::class.java),
    )
    controller.register(Protocol.NOW_PLAYING_COVER, scope.getInstance(UpdateCover::class.java))
    controller.register(Protocol.NOW_PLAYING_RATING, scope.getInstance(UpdateRating::class.java))
    controller.register(Protocol.PLAYER_STATUS, scope.getInstance(UpdatePlayerStatus::class.java))
    controller.register(Protocol.PLAYER_STATE, scope.getInstance(UpdatePlayState::class.java))
    controller.register(Protocol.PLAYER_REPEAT, scope.getInstance(UpdateRepeat::class.java))
    controller.register(Protocol.PLAYER_VOLUME, scope.getInstance(UpdateVolume::class.java))
    controller.register(Protocol.PLAYER_MUTE, scope.getInstance(UpdateMute::class.java))
    controller.register(Protocol.PLAYER_SHUFFLE, scope.getInstance(UpdateShuffle::class.java))
    controller.register(Protocol.PLAYER_SCROBBLE, scope.getInstance(UpdateLastFm::class.java))
    controller.register(Protocol.NOW_PLAYING_LYRICS, scope.getInstance(UpdateLyrics::class.java))
    controller.register(
      Protocol.NOW_PLAYING_LFM_RATING,
      scope.getInstance(UpdateLfmRating::class.java),
    )
    controller.register(
      Protocol.NOW_PLAYING_LIST_REMOVE,
      scope.getInstance(UpdateNowPlayingTrackRemoval::class.java),
    )
    controller.register(
      Protocol.NOW_PLAYING_LIST_MOVE,
      scope.getInstance(UpdateNowPlayingTrackMoved::class.java),
    )
    controller.register(
      Protocol.NOW_PLAYING_POSITION,
      scope.getInstance(UpdatePlaybackPositionCommand::class.java),
    )
    controller.register(
      Protocol.PLUGIN_VERSION,
      scope.getInstance(UpdatePluginVersionCommand::class.java),
    )
    controller.register(Protocol.PING, scope.getInstance(ProtocolPingHandle::class.java))
    controller.register(Protocol.PONG, scope.getInstance(SimpleLogCommand::class.java))

    controller.register(
      UserInputEventType.SETTINGS_CHANGED,
      scope.getInstance(RestartConnectionCommand::class.java),
    )
    controller.register(
      UserInputEventType.CANCEL_NOTIFICATION,
      scope.getInstance(CancelNotificationCommand::class.java),
    )
    controller.register(
      UserInputEventType.START_CONNECTION,
      scope.getInstance(InitiateConnectionCommand::class.java),
    )
    controller.register(
      UserInputEventType.TERMINATE_CONNECTION,
      scope.getInstance(TerminateConnectionCommand::class.java),
    )
    controller.register(
      UserInputEventType.RESET_CONNECTION,
      scope.getInstance(RestartConnectionCommand::class.java),
    )
    controller.register(
      UserInputEventType.START_DISCOVERY,
      scope.getInstance(StartDiscoveryCommand::class.java),
    )
    controller.register(
      UserInputEventType.KEY_VOLUME_UP,
      scope.getInstance(KeyVolumeUpCommand::class.java),
    )
    controller.register(
      UserInputEventType.KEY_VOLUME_DOWN,
      scope.getInstance(KeyVolumeDownCommand::class.java),
    )
    controller.register(
      ApplicationEvents.SOCKET_DATA_AVAILABLE,
      scope.getInstance(SocketDataAvailableCommand::class.java),
    )
    controller.register(
      ApplicationEvents.SOCKET_STATUS_CHANGED,
      scope.getInstance(ConnectionStatusChangedCommand::class.java),
    )
    controller.register(
      ApplicationEvents.SOCKET_HANDSHAKE_UPDATE,
      scope.getInstance(HandleHandshake::class.java),
    )
    controller.register(
      ApplicationEvents.TERMINATE_SERVICE,
      scope.getInstance(TerminateServiceCommand::class.java),
    )
  }

  fun unregister(controller: RemoteController) {
    controller.clearCommands()
  }
}
