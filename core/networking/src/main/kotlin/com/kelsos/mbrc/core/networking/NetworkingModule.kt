package com.kelsos.mbrc.core.networking

import com.kelsos.mbrc.core.networking.api.ContentApi
import com.kelsos.mbrc.core.networking.api.ContentApiImpl
import com.kelsos.mbrc.core.networking.api.LibraryApi
import com.kelsos.mbrc.core.networking.api.LibraryApiImpl
import com.kelsos.mbrc.core.networking.api.OutputApi
import com.kelsos.mbrc.core.networking.api.OutputApiImpl
import com.kelsos.mbrc.core.networking.api.PlaybackApi
import com.kelsos.mbrc.core.networking.api.PlaybackApiImpl
import com.kelsos.mbrc.core.networking.api.QueueApi
import com.kelsos.mbrc.core.networking.api.QueueApiImpl
import com.kelsos.mbrc.core.networking.client.DroppedCommandNotice
import com.kelsos.mbrc.core.networking.client.DroppedCommandNoticeImpl
import com.kelsos.mbrc.core.networking.client.MessageQueue
import com.kelsos.mbrc.core.networking.client.MessageQueueImpl
import com.kelsos.mbrc.core.networking.client.PendingCommandBuffer
import com.kelsos.mbrc.core.networking.client.UiMessageQueue
import com.kelsos.mbrc.core.networking.client.UiMessageQueueImpl
import com.kelsos.mbrc.core.networking.data.DeserializationAdapter
import com.kelsos.mbrc.core.networking.data.DeserializationAdapterImpl
import com.kelsos.mbrc.core.networking.data.SerializationAdapter
import com.kelsos.mbrc.core.networking.data.SerializationAdapterImpl
import com.kelsos.mbrc.core.networking.discovery.DiscoveryNetwork
import com.kelsos.mbrc.core.networking.discovery.DiscoveryTiming
import com.kelsos.mbrc.core.networking.discovery.RemoteServiceDiscovery
import com.kelsos.mbrc.core.networking.discovery.RemoteServiceDiscoveryImpl
import com.kelsos.mbrc.core.networking.discovery.SystemDiscoveryNetwork
import com.kelsos.mbrc.core.networking.protocol.Clock
import com.kelsos.mbrc.core.networking.protocol.SelfMutationConfig
import com.kelsos.mbrc.core.networking.protocol.SelfMutationTracker
import com.kelsos.mbrc.core.networking.protocol.actions.ProtocolPingHandle
import com.kelsos.mbrc.core.networking.protocol.actions.ProtocolVersionUpdate
import com.kelsos.mbrc.core.networking.protocol.actions.SimpleLogCommand
import com.kelsos.mbrc.core.networking.protocol.actions.UpdateCover
import com.kelsos.mbrc.core.networking.protocol.actions.UpdateLastFm
import com.kelsos.mbrc.core.networking.protocol.actions.UpdateLfmRating
import com.kelsos.mbrc.core.networking.protocol.actions.UpdateLyrics
import com.kelsos.mbrc.core.networking.protocol.actions.UpdateMute
import com.kelsos.mbrc.core.networking.protocol.actions.UpdateNowPlayingDetails
import com.kelsos.mbrc.core.networking.protocol.actions.UpdateNowPlayingList
import com.kelsos.mbrc.core.networking.protocol.actions.UpdateNowPlayingTrack
import com.kelsos.mbrc.core.networking.protocol.actions.UpdateNowPlayingTrackMoved
import com.kelsos.mbrc.core.networking.protocol.actions.UpdateNowPlayingTrackRemoval
import com.kelsos.mbrc.core.networking.protocol.actions.UpdatePlayState
import com.kelsos.mbrc.core.networking.protocol.actions.UpdatePlaybackPositionCommand
import com.kelsos.mbrc.core.networking.protocol.actions.UpdatePlayerStatus
import com.kelsos.mbrc.core.networking.protocol.actions.UpdatePluginVersionCommand
import com.kelsos.mbrc.core.networking.protocol.actions.UpdateRating
import com.kelsos.mbrc.core.networking.protocol.actions.UpdateRepeat
import com.kelsos.mbrc.core.networking.protocol.actions.UpdateShuffle
import com.kelsos.mbrc.core.networking.protocol.actions.UpdateVolume
import com.kelsos.mbrc.core.networking.protocol.usecases.CommandFactory
import com.kelsos.mbrc.core.networking.protocol.usecases.CommandFactoryImpl
import com.kelsos.mbrc.core.networking.protocol.usecases.UserActionUseCase
import com.kelsos.mbrc.core.networking.protocol.usecases.UserActionUseCaseImpl
import com.kelsos.mbrc.core.networking.protocol.usecases.VolumeModifyUseCase
import com.kelsos.mbrc.core.networking.protocol.usecases.VolumeModifyUseCaseImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin module for core networking dependencies.
 *
 * This module provides:
 * - API implementations for server communication
 * - Message queue and serialization
 * - Protocol actions for handling server messages
 * - Discovery service for finding MusicBee instances
 *
 * Required dependencies from other modules:
 * - [com.squareup.moshi.Moshi] - JSON serialization
 * - [com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers] - Coroutine dispatchers
 * - [ConnectionProvider] - Provides active connection settings
 * - [ClientIdProvider] - Provides client identification
 * - [DefaultConnectionProvider] - Provides default connection
 * - [LibrarySyncTrigger] - Triggers library sync after connection
 * - [ProtocolActionFactory] - Creates protocol actions
 * - Protocol action handlers (PlayerStateHandler, TrackChangeNotifier, etc.)
 */
val networkingModule = module {
  // Serialization
  singleOf(::SerializationAdapterImpl) { bind<SerializationAdapter>() }
  singleOf(::DeserializationAdapterImpl) { bind<DeserializationAdapter>() }

  // Core networking
  single<Clock> { Clock { System.currentTimeMillis() } }
  single { SelfMutationConfig() }
  singleOf(::SelfMutationTracker)
  singleOf(::ApiBase)
  singleOf(::RequestManagerImpl) { bind<RequestManager>() }
  singleOf(::SocketActivityChecker)
  singleOf(::MessageQueueImpl) { bind<MessageQueue>() }
  singleOf(::UiMessageQueueImpl) { bind<UiMessageQueue>() }
  single<DroppedCommandNotice> { DroppedCommandNoticeImpl() }
  single {
    val droppedCommandNotice = get<DroppedCommandNotice>()
    PendingCommandBuffer(get(), onDiscarded = droppedCommandNotice::record)
  }

  // Connection management
  singleOf(::ClientConnectionManagerImpl) { bind<ClientConnectionManager>() }
  singleOf(::ClientConnectionUseCaseImpl) { bind<ClientConnectionUseCase>() }
  singleOf(::MessageHandlerImpl) { bind<MessageHandler>() }

  // Discovery
  singleOf(::SystemDiscoveryNetwork) { bind<DiscoveryNetwork>() }
  single { DiscoveryTiming.SHIPPED }
  singleOf(::RemoteServiceDiscoveryImpl) { bind<RemoteServiceDiscovery>() }

  // Domain-specific API implementations
  singleOf(::LibraryApiImpl) { bind<LibraryApi>() }
  singleOf(::QueueApiImpl) { bind<QueueApi>() }
  singleOf(::PlaybackApiImpl) { bind<PlaybackApi>() }
  singleOf(::ContentApiImpl) { bind<ContentApi>() }
  singleOf(::OutputApiImpl) { bind<OutputApi>() }

  // Command factory
  single<CommandFactory> { CommandFactoryImpl { kClass -> get(kClass) } }

  // Use cases
  singleOf(::UserActionUseCaseImpl) { bind<UserActionUseCase>() }
  singleOf(::VolumeModifyUseCaseImpl) { bind<VolumeModifyUseCase>() }

  // Protocol actions
  factoryOf(::UpdateNowPlayingTrack)
  factoryOf(::UpdateCover)
  factoryOf(::UpdateRating)
  factoryOf(::UpdatePlayerStatus)
  factoryOf(::UpdatePlayState)
  factoryOf(::UpdateRepeat)
  factoryOf(::UpdateVolume)
  factoryOf(::UpdateMute)
  factoryOf(::UpdateShuffle)
  factoryOf(::UpdateLastFm)
  factoryOf(::UpdateLfmRating)
  factoryOf(::UpdateLyrics)
  factoryOf(::UpdateNowPlayingTrackMoved)
  factoryOf(::UpdateNowPlayingTrackRemoval)
  factoryOf(::UpdateNowPlayingList)
  factoryOf(::UpdateNowPlayingDetails)
  factoryOf(::UpdatePlaybackPositionCommand)
  factoryOf(::UpdatePluginVersionCommand)
  factoryOf(::ProtocolPingHandle)
  factoryOf(::SimpleLogCommand)
  factoryOf(::ProtocolVersionUpdate)
}
