package com.kelsos.mbrc

import androidx.core.app.NotificationManagerCompat
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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
import com.kelsos.mbrc.common.state.ConnectionModel
import com.kelsos.mbrc.common.state.MainDataModel
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.bus.RxBusImpl
import com.kelsos.mbrc.features.library.AlbumEntryAdapter
import com.kelsos.mbrc.features.library.AlbumRepository
import com.kelsos.mbrc.features.library.AlbumRepositoryImpl
import com.kelsos.mbrc.features.library.AlbumTracksActivity
import com.kelsos.mbrc.features.library.AlbumTracksPresenter
import com.kelsos.mbrc.features.library.AlbumTracksPresenterImpl
import com.kelsos.mbrc.features.library.ArtistAlbumsActivity
import com.kelsos.mbrc.features.library.ArtistAlbumsPresenter
import com.kelsos.mbrc.features.library.ArtistAlbumsPresenterImpl
import com.kelsos.mbrc.features.library.ArtistEntryAdapter
import com.kelsos.mbrc.features.library.ArtistRepository
import com.kelsos.mbrc.features.library.ArtistRepositoryImpl
import com.kelsos.mbrc.features.library.BrowseAlbumFragment
import com.kelsos.mbrc.features.library.BrowseAlbumPresenter
import com.kelsos.mbrc.features.library.BrowseAlbumPresenterImpl
import com.kelsos.mbrc.features.library.BrowseArtistFragment
import com.kelsos.mbrc.features.library.BrowseArtistPresenter
import com.kelsos.mbrc.features.library.BrowseArtistPresenterImpl
import com.kelsos.mbrc.features.library.BrowseGenreFragment
import com.kelsos.mbrc.features.library.BrowseGenrePresenter
import com.kelsos.mbrc.features.library.BrowseGenrePresenterImpl
import com.kelsos.mbrc.features.library.BrowseTrackFragment
import com.kelsos.mbrc.features.library.BrowseTrackPresenter
import com.kelsos.mbrc.features.library.BrowseTrackPresenterImpl
import com.kelsos.mbrc.features.library.CoverCache
import com.kelsos.mbrc.features.library.GenreArtistsActivity
import com.kelsos.mbrc.features.library.GenreArtistsPresenter
import com.kelsos.mbrc.features.library.GenreArtistsPresenterImpl
import com.kelsos.mbrc.features.library.GenreEntryAdapter
import com.kelsos.mbrc.features.library.GenreRepository
import com.kelsos.mbrc.features.library.GenreRepositoryImpl
import com.kelsos.mbrc.features.library.LibraryActivity
import com.kelsos.mbrc.features.library.LibraryPresenter
import com.kelsos.mbrc.features.library.LibraryPresenterImpl
import com.kelsos.mbrc.features.library.LibrarySearchModel
import com.kelsos.mbrc.features.library.LibrarySyncUseCase
import com.kelsos.mbrc.features.library.LibrarySyncUseCaseImpl
import com.kelsos.mbrc.features.library.LocalAlbumDataSource
import com.kelsos.mbrc.features.library.LocalArtistDataSource
import com.kelsos.mbrc.features.library.LocalArtistDataSourceImpl
import com.kelsos.mbrc.features.library.LocalGenreDataSource
import com.kelsos.mbrc.features.library.LocalTrackDataSource
import com.kelsos.mbrc.features.library.RemoteAlbumDataSource
import com.kelsos.mbrc.features.library.RemoteArtistDataSource
import com.kelsos.mbrc.features.library.RemoteGenreDataSource
import com.kelsos.mbrc.features.library.RemoteTrackDataSource
import com.kelsos.mbrc.features.library.TrackEntryAdapter
import com.kelsos.mbrc.features.library.TrackRepository
import com.kelsos.mbrc.features.library.TrackRepositoryImpl
import com.kelsos.mbrc.features.lyrics.LyricsActivity
import com.kelsos.mbrc.features.lyrics.LyricsModel
import com.kelsos.mbrc.features.lyrics.LyricsPresenter
import com.kelsos.mbrc.features.lyrics.LyricsPresenterImpl
import com.kelsos.mbrc.features.minicontrol.MiniControlFragment
import com.kelsos.mbrc.features.minicontrol.MiniControlPresenter
import com.kelsos.mbrc.features.minicontrol.MiniControlPresenterImpl
import com.kelsos.mbrc.features.nowplaying.LocalNowPlayingDataSource
import com.kelsos.mbrc.features.nowplaying.NowPlayingActivity
import com.kelsos.mbrc.features.nowplaying.NowPlayingAdapter
import com.kelsos.mbrc.features.nowplaying.NowPlayingPresenter
import com.kelsos.mbrc.features.nowplaying.NowPlayingPresenterImpl
import com.kelsos.mbrc.features.nowplaying.NowPlayingRepository
import com.kelsos.mbrc.features.nowplaying.NowPlayingRepositoryImpl
import com.kelsos.mbrc.features.nowplaying.RemoteNowPlayingDataSource
import com.kelsos.mbrc.features.output.OutputApi
import com.kelsos.mbrc.features.output.OutputApiImpl
import com.kelsos.mbrc.features.output.OutputSelectionViewModel
import com.kelsos.mbrc.features.player.ModelCache
import com.kelsos.mbrc.features.player.ModelCacheImpl
import com.kelsos.mbrc.features.player.ModelInitializer
import com.kelsos.mbrc.features.player.PlayerActivity
import com.kelsos.mbrc.features.player.PlayerViewPresenter
import com.kelsos.mbrc.features.player.PlayerViewPresenterImpl
import com.kelsos.mbrc.features.player.ProgressSeekerHelper
import com.kelsos.mbrc.features.playlists.LocalPlaylistDataSource
import com.kelsos.mbrc.features.playlists.PlaylistActivity
import com.kelsos.mbrc.features.playlists.PlaylistAdapter
import com.kelsos.mbrc.features.playlists.PlaylistPresenter
import com.kelsos.mbrc.features.playlists.PlaylistPresenterImpl
import com.kelsos.mbrc.features.playlists.PlaylistRepository
import com.kelsos.mbrc.features.playlists.PlaylistRepositoryImpl
import com.kelsos.mbrc.features.playlists.RemotePlaylistDataSource
import com.kelsos.mbrc.features.queue.PopupActionHandler
import com.kelsos.mbrc.features.queue.QueueHandler
import com.kelsos.mbrc.features.radio.LocalRadioDataSource
import com.kelsos.mbrc.features.radio.RadioActivity
import com.kelsos.mbrc.features.radio.RadioAdapter
import com.kelsos.mbrc.features.radio.RadioPresenter
import com.kelsos.mbrc.features.radio.RadioPresenterImpl
import com.kelsos.mbrc.features.radio.RadioRepository
import com.kelsos.mbrc.features.radio.RadioRepositoryImpl
import com.kelsos.mbrc.features.radio.RemoteRadioDataSource
import com.kelsos.mbrc.features.settings.BasicSettingsHelper
import com.kelsos.mbrc.features.settings.ConnectionManagerActivity
import com.kelsos.mbrc.features.settings.ConnectionManagerPresenter
import com.kelsos.mbrc.features.settings.ConnectionManagerPresenterImpl
import com.kelsos.mbrc.features.settings.ConnectionRepository
import com.kelsos.mbrc.features.settings.ConnectionRepositoryImpl
import com.kelsos.mbrc.features.settings.SettingsManager
import com.kelsos.mbrc.features.settings.SettingsManagerImpl
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.RequestManager
import com.kelsos.mbrc.networking.RequestManagerImpl
import com.kelsos.mbrc.networking.SocketActivityChecker
import com.kelsos.mbrc.networking.client.SocketService
import com.kelsos.mbrc.networking.discovery.RemoteServiceDiscovery
import com.kelsos.mbrc.networking.protocol.ProtocolHandler
import com.kelsos.mbrc.networking.protocol.ProtocolPingHandle
import com.kelsos.mbrc.networking.protocol.RemoteController
import com.kelsos.mbrc.networking.protocol.SimpleLogCommand
import com.kelsos.mbrc.networking.protocol.UpdateCover
import com.kelsos.mbrc.networking.protocol.UpdateLastFm
import com.kelsos.mbrc.networking.protocol.UpdateLfmRating
import com.kelsos.mbrc.networking.protocol.UpdateLyrics
import com.kelsos.mbrc.networking.protocol.UpdateMute
import com.kelsos.mbrc.networking.protocol.UpdateNowPlayingTrack
import com.kelsos.mbrc.networking.protocol.UpdateNowPlayingTrackMoved
import com.kelsos.mbrc.networking.protocol.UpdateNowPlayingTrackRemoval
import com.kelsos.mbrc.networking.protocol.UpdatePlayState
import com.kelsos.mbrc.networking.protocol.UpdatePlaybackPositionCommand
import com.kelsos.mbrc.networking.protocol.UpdatePlayerStatus
import com.kelsos.mbrc.networking.protocol.UpdatePluginVersionCommand
import com.kelsos.mbrc.networking.protocol.UpdateRating
import com.kelsos.mbrc.networking.protocol.UpdateRepeat
import com.kelsos.mbrc.networking.protocol.UpdateShuffle
import com.kelsos.mbrc.networking.protocol.UpdateVolume
import com.kelsos.mbrc.platform.RemoteBroadcastReceiver
import com.kelsos.mbrc.platform.ServiceChecker
import com.kelsos.mbrc.platform.ServiceCheckerImpl
import com.kelsos.mbrc.platform.mediasession.NotificationModel
import com.kelsos.mbrc.platform.mediasession.RemoteSessionManager
import com.kelsos.mbrc.platform.mediasession.RemoteVolumeProvider
import com.kelsos.mbrc.platform.mediasession.SessionNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.concurrent.Executors

val appModule =
  module {
    singleOf(::RxBusImpl) { bind<RxBus>() }
    single { ObjectMapper().registerKotlinModule() }

    singleOf(::LibrarySyncUseCaseImpl) { bind<LibrarySyncUseCase>() }
    singleOf(::ApiBase)
    singleOf(::RequestManagerImpl) { bind<RequestManager>() }
    singleOf(::OutputApiImpl) { bind<OutputApi>() }

    singleOf(::GenreRepositoryImpl) { bind<GenreRepository>() }
    singleOf(::ArtistRepositoryImpl) { bind<ArtistRepository>() }
    singleOf(::LocalArtistDataSourceImpl) { bind<LocalArtistDataSource>() }
    singleOf(::AlbumRepositoryImpl) { bind<AlbumRepository>() }
    singleOf(::TrackRepositoryImpl) { bind<TrackRepository>() }

    singleOf(::NowPlayingRepositoryImpl) { bind<NowPlayingRepository>() }
    singleOf(::PlaylistRepositoryImpl) { bind<PlaylistRepository>() }
    singleOf(::RadioRepositoryImpl) { bind<RadioRepository>() }
    singleOf(::ConnectionRepositoryImpl) { bind<ConnectionRepository>() }

    singleOf(::LocalGenreDataSource)
    singleOf(::RemoteGenreDataSource)
    singleOf(::LocalArtistDataSourceImpl) { bind<LocalArtistDataSource>() }
    singleOf(::RemoteArtistDataSource)
    singleOf(::LocalAlbumDataSource)
    singleOf(::RemoteAlbumDataSource)
    singleOf(::LocalTrackDataSource)
    singleOf(::RemoteTrackDataSource)
    singleOf(::LocalPlaylistDataSource)
    singleOf(::RemotePlaylistDataSource)
    singleOf(::LocalNowPlayingDataSource)
    singleOf(::RemoteNowPlayingDataSource)
    singleOf(::LocalRadioDataSource)
    singleOf(::RemoteRadioDataSource)

    singleOf(::GenreEntryAdapter)
    singleOf(::ArtistEntryAdapter)

    singleOf(::QueueHandler)

    single<NotificationManagerCompat> {
      NotificationManagerCompat.from(get())
    }

    singleOf(::RemoteBroadcastReceiver)
    singleOf(::SettingsManagerImpl) { bind<SettingsManager>() }
    singleOf(::ServiceCheckerImpl) { bind<ServiceChecker>() }
    singleOf(::ModelCacheImpl) { bind<ModelCache>() }
    singleOf(::MainDataModel)
    singleOf(::ModelInitializer)
    singleOf(::ConnectionModel)
    singleOf(::LyricsModel)
    singleOf(::RemoteController)
    singleOf(::SocketService)
    singleOf(::SocketActivityChecker)
    singleOf(::CoverCache)
    singleOf(::ProtocolHandler)
    singleOf(::NotificationModel)

    factoryOf(::BasicSettingsHelper)

    factoryOf(::ReduceVolumeOnRingCommand)
    factoryOf(::HandshakeCompletionActions)
    factoryOf(::NotifyNotAllowedCommand)
    factoryOf(::ProtocolRequest)
    factoryOf(::VersionCheckCommand)
    factoryOf(::ProcessUserAction)
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
    factoryOf(::UpdatePlaybackPositionCommand)
    factoryOf(::UpdatePluginVersionCommand)
    factoryOf(::ProtocolPingHandle)
    factoryOf(::SimpleLogCommand)
    factoryOf(::RestartConnectionCommand)
    factoryOf(::CancelNotificationCommand)
    factoryOf(::SessionNotificationManager)
    factoryOf(::RemoteSessionManager)
    factoryOf(::RemoteVolumeProvider)
    factoryOf(::InitiateConnectionCommand)
    factoryOf(::TerminateConnectionCommand)
    factoryOf(::StartDiscoveryCommand)
    factoryOf(::RemoteServiceDiscovery)
    factoryOf(::KeyVolumeUpCommand)
    factoryOf(::KeyVolumeDownCommand)
    factoryOf(::SocketDataAvailableCommand)
    factoryOf(::ConnectionStatusChangedCommand)
    factoryOf(::HandleHandshake)
    factoryOf(::TerminateServiceCommand)

    single<Scheduler>(named("main")) { AndroidSchedulers.mainThread() }
    single<Scheduler>(named("io")) { Schedulers.io() }

    single {
      val database =
        Executors
          .newSingleThreadExecutor { runnable ->
            Thread(runnable, "DatabaseDispatcher")
          }.asCoroutineDispatcher()

      var threadId = 1

      val network =
        Executors
          .newFixedThreadPool(2) { runnable ->
            Thread(runnable, "NetworkDispatcher-worker-${threadId++}")
          }.asCoroutineDispatcher()

      AppCoroutineDispatchers(
        main = Dispatchers.Main,
        io = Dispatchers.IO,
        database = database,
        network = network,
      )
    }

    singleOf(::ArtistAlbumsPresenterImpl) { bind<ArtistAlbumsPresenter>() }

    scope<MiniControlFragment> {
      scopedOf(::MiniControlPresenterImpl) { bind<MiniControlPresenter>() }
    }

    scope<PlayerActivity> {
      scopedOf(::PlayerViewPresenterImpl) { bind<PlayerViewPresenter>() }
      scoped<ProgressSeekerHelper> { ProgressSeekerHelper(get(named("main"))) }
    }

    scope<LyricsActivity> {
      scopedOf(::LyricsPresenterImpl) { bind<LyricsPresenter>() }
    }

    scope<LibraryActivity> {
      scopedOf(::LibraryPresenterImpl) { bind<LibraryPresenter>() }
      scopedOf(::LibrarySearchModel)
      scopedOf(::PopupActionHandler)
    }

    scope<BrowseGenreFragment> {
      scopedOf(::BrowseGenrePresenterImpl) { bind<BrowseGenrePresenter>() }
      scopedOf(::GenreEntryAdapter)
    }

    scope<BrowseArtistFragment> {
      scopedOf(::BrowseArtistPresenterImpl) { bind<BrowseArtistPresenter>() }
      scopedOf(::ArtistEntryAdapter)
    }

    scope<BrowseAlbumFragment> {
      scopedOf(::BrowseAlbumPresenterImpl) { bind<BrowseAlbumPresenter>() }
      scopedOf(::AlbumEntryAdapter)
    }

    scope<BrowseTrackFragment> {
      scopedOf(::BrowseTrackPresenterImpl) { bind<BrowseTrackPresenter>() }
      scopedOf(::TrackEntryAdapter)
    }

    scope<GenreArtistsActivity> {
      scopedOf(::GenreArtistsPresenterImpl) { bind<GenreArtistsPresenter>() }
      scopedOf(::ArtistEntryAdapter)
      scopedOf(::PopupActionHandler)
    }

    scope<ArtistAlbumsActivity> {
      scopedOf(::ArtistAlbumsPresenterImpl) { bind<ArtistAlbumsPresenter>() }
      scopedOf(::AlbumEntryAdapter)
      scopedOf(::PopupActionHandler)
    }

    scope<AlbumTracksActivity> {
      scopedOf(::AlbumTracksPresenterImpl) { bind<AlbumTracksPresenter>() }
      scopedOf(::TrackEntryAdapter)
      scopedOf(::PopupActionHandler)
    }

    scope<NowPlayingActivity> {
      scopedOf(::NowPlayingPresenterImpl) { bind<NowPlayingPresenter>() }
      scopedOf(::NowPlayingAdapter)
    }

    scope<PlaylistActivity> {
      scopedOf(::PlaylistPresenterImpl) { bind<PlaylistPresenter>() }
      scopedOf(::PlaylistAdapter)
    }

    scope<ConnectionManagerActivity> {
      scopedOf(::ConnectionManagerPresenterImpl) { bind<ConnectionManagerPresenter>() }
    }

    scope<RadioActivity> {
      scopedOf(::RadioPresenterImpl) { bind<RadioPresenter>() }
      scopedOf(::RadioAdapter)
    }

    viewModelOf(::OutputSelectionViewModel)
  }
