package com.kelsos.mbrc.networking.protocol

import org.koin.core.component.KoinComponent

interface CommandFactory {
  fun create(protocol: Protocol): ProtocolAction
}

class CommandFactoryImpl :
  CommandFactory,
  KoinComponent {
  private val protocolActionMapping =
    mapOf(
      Protocol.NowPlayingTrack to UpdateNowPlayingTrack::class,
      Protocol.NowPlayingCover to UpdateCover::class,
      Protocol.NowPlayingRating to UpdateRating::class,
      Protocol.PlayerStatus to UpdatePlayerStatus::class,
      Protocol.PlayerState to UpdatePlayState::class,
      Protocol.PlayerRepeat to UpdateRepeat::class,
      Protocol.PlayerVolume to UpdateVolume::class,
      Protocol.PlayerMute to UpdateMute::class,
      Protocol.PlayerShuffle to UpdateShuffle::class,
      Protocol.PlayerScrobble to UpdateLastFm::class,
      Protocol.NowPlayingLyrics to UpdateLyrics::class,
      Protocol.NowPlayingLfmRating to UpdateLfmRating::class,
      Protocol.NowPlayingListRemove to UpdateNowPlayingTrackRemoval::class,
      Protocol.NowPlayingListMove to UpdateNowPlayingTrackMoved::class,
      Protocol.NowPlayingPosition to UpdatePlaybackPositionCommand::class,
      Protocol.NowPlayingListChanged to UpdateNowPlayingList::class,
      Protocol.PluginVersion to UpdatePluginVersionCommand::class,
      Protocol.Ping to ProtocolPingHandle::class,
      Protocol.Pong to SimpleLogCommand::class,
      Protocol.PlayerNext to SimpleLogCommand::class,
      Protocol.PlayerPrevious to SimpleLogCommand::class,
      Protocol.PlayerPlayPause to SimpleLogCommand::class,
      Protocol.NowPlayingListPlay to SimpleLogCommand::class,
      Protocol.ProtocolTag to ProtocolVersionUpdate::class
    )

  private fun resolveAction(protocol: Protocol): ProtocolAction? {
    val commandClass = protocolActionMapping[protocol]
    return commandClass?.let { getKoin().get(it) }
  }

  override fun create(protocol: Protocol): ProtocolAction {
    val resolvedAction = resolveAction(protocol)
    return resolvedAction ?: error("Not supported message context $protocol")
  }
}
