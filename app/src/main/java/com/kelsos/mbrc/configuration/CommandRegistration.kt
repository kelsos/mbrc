package com.kelsos.mbrc.configuration

import com.kelsos.mbrc.commands.CancelNotificationCommand
import com.kelsos.mbrc.commands.ConnectionStatusChangedCommand
import com.kelsos.mbrc.commands.HandleHandshake
import com.kelsos.mbrc.commands.InitiateConnectionCommand
import com.kelsos.mbrc.commands.KeyVolumeDownCommand
import com.kelsos.mbrc.commands.KeyVolumeUpCommand
import com.kelsos.mbrc.commands.ProcessUserAction
import com.kelsos.mbrc.commands.ProtocolPingHandle
import com.kelsos.mbrc.commands.ProtocolPongHandle
import com.kelsos.mbrc.commands.ProtocolRequest
import com.kelsos.mbrc.commands.ReduceVolumeOnRingCommand
import com.kelsos.mbrc.commands.RestartConnectionCommand
import com.kelsos.mbrc.commands.SocketDataAvailableCommand
import com.kelsos.mbrc.commands.StartDiscoveryCommand
import com.kelsos.mbrc.commands.TerminateConnectionCommand
import com.kelsos.mbrc.commands.TerminateServiceCommand
import com.kelsos.mbrc.commands.VersionCheckCommand
import com.kelsos.mbrc.commands.model.UpdateCover
import com.kelsos.mbrc.commands.model.UpdateLastFm
import com.kelsos.mbrc.commands.model.UpdateLfmRating
import com.kelsos.mbrc.commands.model.UpdateLyrics
import com.kelsos.mbrc.commands.model.UpdateMute
import com.kelsos.mbrc.commands.model.UpdateNowPlayingTrack
import com.kelsos.mbrc.commands.model.UpdatePlayState
import com.kelsos.mbrc.commands.model.UpdatePlayerStatus
import com.kelsos.mbrc.commands.model.UpdatePluginVersionCommand
import com.kelsos.mbrc.commands.model.UpdateRating
import com.kelsos.mbrc.commands.model.UpdateRepeat
import com.kelsos.mbrc.commands.model.UpdateShuffle
import com.kelsos.mbrc.commands.model.UpdateVolume
import com.kelsos.mbrc.commands.visual.HandshakeCompletionActions
import com.kelsos.mbrc.commands.visual.NotifyNotAllowedCommand
import com.kelsos.mbrc.commands.visual.UpdateNowPlayingTrackMoved
import com.kelsos.mbrc.commands.visual.UpdateNowPlayingTrackRemoval
import com.kelsos.mbrc.commands.visual.UpdatePlaybackPositionCommand
import com.kelsos.mbrc.constants.ApplicationEvents
import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.constants.ProtocolEventType
import com.kelsos.mbrc.constants.UserInputEventType
import com.kelsos.mbrc.controller.RemoteController
import toothpick.Scope

object CommandRegistration {
  fun register(
    controller: RemoteController,
    scope: Scope,
  ) {
    controller.register(
      ProtocolEventType.ReduceVolume,
      scope.getInstance(ReduceVolumeOnRingCommand::class.java),
    )
    controller.register(
      ProtocolEventType.HandshakeComplete,
      scope.getInstance(HandshakeCompletionActions::class.java),
    )
    controller.register(
      ProtocolEventType.InformClientNotAllowed,
      scope.getInstance(NotifyNotAllowedCommand::class.java),
    )
    controller.register(
      ProtocolEventType.InitiateProtocolRequest,
      scope.getInstance(ProtocolRequest::class.java),
    )
    controller.register(
      ProtocolEventType.PluginVersionCheck,
      scope.getInstance(VersionCheckCommand::class.java),
    )
    controller.register(
      ProtocolEventType.UserAction,
      scope.getInstance(ProcessUserAction::class.java),
    )

    controller.register(
      Protocol.NowPlayingTrack,
      scope.getInstance(UpdateNowPlayingTrack::class.java),
    )
    controller.register(Protocol.NowPlayingCover, scope.getInstance(UpdateCover::class.java))
    controller.register(Protocol.NowPlayingRating, scope.getInstance(UpdateRating::class.java))
    controller.register(Protocol.PlayerStatus, scope.getInstance(UpdatePlayerStatus::class.java))
    controller.register(Protocol.PlayerState, scope.getInstance(UpdatePlayState::class.java))
    controller.register(Protocol.PlayerRepeat, scope.getInstance(UpdateRepeat::class.java))
    controller.register(Protocol.PlayerVolume, scope.getInstance(UpdateVolume::class.java))
    controller.register(Protocol.PlayerMute, scope.getInstance(UpdateMute::class.java))
    controller.register(Protocol.PlayerShuffle, scope.getInstance(UpdateShuffle::class.java))
    controller.register(Protocol.PlayerScrobble, scope.getInstance(UpdateLastFm::class.java))
    controller.register(Protocol.NowPlayingLyrics, scope.getInstance(UpdateLyrics::class.java))
    controller.register(
      Protocol.NowPlayingLfmRating,
      scope.getInstance(UpdateLfmRating::class.java),
    )
    controller.register(
      Protocol.NowPlayingListRemove,
      scope.getInstance(UpdateNowPlayingTrackRemoval::class.java),
    )
    controller.register(
      Protocol.NowPlayingListMove,
      scope.getInstance(UpdateNowPlayingTrackMoved::class.java),
    )
    controller.register(
      Protocol.NowPlayingPosition,
      scope.getInstance(UpdatePlaybackPositionCommand::class.java),
    )
    controller.register(
      Protocol.PluginVersion,
      scope.getInstance(UpdatePluginVersionCommand::class.java),
    )
    controller.register(Protocol.PING, scope.getInstance(ProtocolPingHandle::class.java))
    controller.register(Protocol.PONG, scope.getInstance(ProtocolPongHandle::class.java))

    controller.register(
      UserInputEventType.SettingsChanged,
      scope.getInstance(RestartConnectionCommand::class.java),
    )
    controller.register(
      UserInputEventType.CancelNotification,
      scope.getInstance(CancelNotificationCommand::class.java),
    )
    controller.register(
      UserInputEventType.StartConnection,
      scope.getInstance(InitiateConnectionCommand::class.java),
    )
    controller.register(
      UserInputEventType.TerminateConnection,
      scope.getInstance(TerminateConnectionCommand::class.java),
    )
    controller.register(
      UserInputEventType.ResetConnection,
      scope.getInstance(RestartConnectionCommand::class.java),
    )
    controller.register(
      UserInputEventType.StartDiscovery,
      scope.getInstance(StartDiscoveryCommand::class.java),
    )
    controller.register(
      UserInputEventType.KeyVolumeUp,
      scope.getInstance(KeyVolumeUpCommand::class.java),
    )
    controller.register(
      UserInputEventType.KeyVolumeDown,
      scope.getInstance(KeyVolumeDownCommand::class.java),
    )
    controller.register(
      ApplicationEvents.SocketDataAvailable,
      scope.getInstance(SocketDataAvailableCommand::class.java),
    )
    controller.register(
      ApplicationEvents.SocketStatusChanged,
      scope.getInstance(ConnectionStatusChangedCommand::class.java),
    )
    controller.register(
      ApplicationEvents.SocketHandshakeUpdate,
      scope.getInstance(HandleHandshake::class.java),
    )
    controller.register(
      ApplicationEvents.TerminateService,
      scope.getInstance(TerminateServiceCommand::class.java),
    )
  }

  fun unregister(controller: RemoteController) {
    controller.clearCommands()
  }
}
