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
import org.koin.core.Koin

object CommandRegistration {
  fun register(
    controller: RemoteController,
    koin: Koin,
  ) {
    controller.register(
      ProtocolEventType.REDUCE_VOLUME,
      koin.get<ReduceVolumeOnRingCommand>(),
    )
    controller.register(
      ProtocolEventType.HANDSHAKE_COMPLETE,
      koin.get<HandshakeCompletionActions>(),
    )
    controller.register(
      ProtocolEventType.INFORM_CLIENT_NOT_ALLOWED,
      koin.get<NotifyNotAllowedCommand>(),
    )
    controller.register(
      ProtocolEventType.INITIATE_PROTOCOL_REQUEST,
      koin.get<ProtocolRequest>(),
    )
    controller.register(
      ProtocolEventType.PLUGIN_VERSION_CHECK,
      koin.get<VersionCheckCommand>(),
    )
    controller.register(
      ProtocolEventType.USER_ACTION,
      koin.get<ProcessUserAction>(),
    )

    controller.register(
      Protocol.NOW_PLAYING_TRACK,
      koin.get<UpdateNowPlayingTrack>(),
    )
    controller.register(Protocol.NOW_PLAYING_COVER, koin.get<UpdateCover>())
    controller.register(Protocol.NOW_PLAYING_RATING, koin.get<UpdateRating>())
    controller.register(Protocol.PLAYER_STATUS, koin.get<UpdatePlayerStatus>())
    controller.register(Protocol.PLAYER_STATE, koin.get<UpdatePlayState>())
    controller.register(Protocol.PLAYER_REPEAT, koin.get<UpdateRepeat>())
    controller.register(Protocol.PLAYER_VOLUME, koin.get<UpdateVolume>())
    controller.register(Protocol.PLAYER_MUTE, koin.get<UpdateMute>())
    controller.register(Protocol.PLAYER_SHUFFLE, koin.get<UpdateShuffle>())
    controller.register(Protocol.PLAYER_SCROBBLE, koin.get<UpdateLastFm>())
    controller.register(Protocol.NOW_PLAYING_LYRICS, koin.get<UpdateLyrics>())
    controller.register(
      Protocol.NOW_PLAYING_LFM_RATING,
      koin.get<UpdateLfmRating>(),
    )
    controller.register(
      Protocol.NOW_PLAYING_LIST_REMOVE,
      koin.get<UpdateNowPlayingTrackRemoval>(),
    )
    controller.register(
      Protocol.NOW_PLAYING_LIST_MOVE,
      koin.get<UpdateNowPlayingTrackMoved>(),
    )
    controller.register(
      Protocol.NOW_PLAYING_POSITION,
      koin.get<UpdatePlaybackPositionCommand>(),
    )
    controller.register(
      Protocol.PLUGIN_VERSION,
      koin.get<UpdatePluginVersionCommand>(),
    )
    controller.register(Protocol.PING, koin.get<ProtocolPingHandle>())
    controller.register(Protocol.PONG, koin.get<SimpleLogCommand>())

    controller.register(
      UserInputEventType.SETTINGS_CHANGED,
      koin.get<RestartConnectionCommand>(),
    )
    controller.register(
      UserInputEventType.CANCEL_NOTIFICATION,
      koin.get<CancelNotificationCommand>(),
    )
    controller.register(
      UserInputEventType.START_CONNECTION,
      koin.get<InitiateConnectionCommand>(),
    )
    controller.register(
      UserInputEventType.TERMINATE_CONNECTION,
      koin.get<TerminateConnectionCommand>(),
    )
    controller.register(
      UserInputEventType.RESET_CONNECTION,
      koin.get<RestartConnectionCommand>(),
    )
    controller.register(
      UserInputEventType.START_DISCOVERY,
      koin.get<StartDiscoveryCommand>(),
    )
    controller.register(
      UserInputEventType.KEY_VOLUME_UP,
      koin.get<KeyVolumeUpCommand>(),
    )
    controller.register(
      UserInputEventType.KEY_VOLUME_DOWN,
      koin.get<KeyVolumeDownCommand>(),
    )
    controller.register(
      ApplicationEvents.SOCKET_DATA_AVAILABLE,
      koin.get<SocketDataAvailableCommand>(),
    )
    controller.register(
      ApplicationEvents.SOCKET_STATUS_CHANGED,
      koin.get<ConnectionStatusChangedCommand>(),
    )
    controller.register(
      ApplicationEvents.SOCKET_HANDSHAKE_UPDATE,
      koin.get<HandleHandshake>(),
    )
    controller.register(
      ApplicationEvents.TERMINATE_SERVICE,
      koin.get<TerminateServiceCommand>(),
    )
  }

  fun unregister(controller: RemoteController) {
    controller.clearCommands()
  }
}
