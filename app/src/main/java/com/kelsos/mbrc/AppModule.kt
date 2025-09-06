package com.kelsos.mbrc

import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import androidx.work.WorkManager
import com.kelsos.mbrc.app.DrawerViewModel
import com.kelsos.mbrc.common.state.AppState
import com.kelsos.mbrc.common.state.AppStateFlow
import com.kelsos.mbrc.common.state.AppStateManager
import com.kelsos.mbrc.common.state.AppStatePublisher
import com.kelsos.mbrc.common.state.ConnectionState
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.common.state.ConnectionStatePublisher
import com.kelsos.mbrc.common.state.PlayingTrackCache
import com.kelsos.mbrc.common.state.PlayingTrackCacheImpl
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.data.Database
import com.kelsos.mbrc.data.DefaultConnectionMigration
import com.kelsos.mbrc.data.DeserializationAdapter
import com.kelsos.mbrc.data.DeserializationAdapterImpl
import com.kelsos.mbrc.data.MIGRATION_3_4
import com.kelsos.mbrc.data.MigrationManager
import com.kelsos.mbrc.data.SerializationAdapter
import com.kelsos.mbrc.data.SerializationAdapterImpl
import com.kelsos.mbrc.features.help.FeedbackFragment
import com.kelsos.mbrc.features.help.FeedbackViewModel
import com.kelsos.mbrc.features.library.CoverCache
import com.kelsos.mbrc.features.library.LibraryActivity
import com.kelsos.mbrc.features.library.LibrarySearchModel
import com.kelsos.mbrc.features.library.LibrarySyncUseCase
import com.kelsos.mbrc.features.library.LibrarySyncUseCaseImpl
import com.kelsos.mbrc.features.library.LibrarySyncWorkHandler
import com.kelsos.mbrc.features.library.LibrarySyncWorkHandlerImpl
import com.kelsos.mbrc.features.library.LibrarySyncWorker
import com.kelsos.mbrc.features.library.LibraryViewModel
import com.kelsos.mbrc.features.library.albums.AlbumEntryAdapter
import com.kelsos.mbrc.features.library.albums.AlbumRepository
import com.kelsos.mbrc.features.library.albums.AlbumRepositoryImpl
import com.kelsos.mbrc.features.library.albums.ArtistAlbumsActivity
import com.kelsos.mbrc.features.library.albums.ArtistAlbumsViewModel
import com.kelsos.mbrc.features.library.albums.BrowseAlbumFragment
import com.kelsos.mbrc.features.library.albums.BrowseAlbumViewModel
import com.kelsos.mbrc.features.library.artists.ArtistEntryAdapter
import com.kelsos.mbrc.features.library.artists.ArtistRepository
import com.kelsos.mbrc.features.library.artists.ArtistRepositoryImpl
import com.kelsos.mbrc.features.library.artists.BrowseArtistFragment
import com.kelsos.mbrc.features.library.artists.BrowseArtistViewModel
import com.kelsos.mbrc.features.library.artists.GenreArtistsActivity
import com.kelsos.mbrc.features.library.artists.GenreArtistsViewModel
import com.kelsos.mbrc.features.library.genres.BrowseGenreFragment
import com.kelsos.mbrc.features.library.genres.BrowseGenreViewModel
import com.kelsos.mbrc.features.library.genres.GenreEntryAdapter
import com.kelsos.mbrc.features.library.genres.GenreRepository
import com.kelsos.mbrc.features.library.genres.GenreRepositoryImpl
import com.kelsos.mbrc.features.library.tracks.AlbumTracksActivity
import com.kelsos.mbrc.features.library.tracks.AlbumTracksViewModel
import com.kelsos.mbrc.features.library.tracks.BrowseTrackFragment
import com.kelsos.mbrc.features.library.tracks.BrowseTrackViewModel
import com.kelsos.mbrc.features.library.tracks.TrackEntryAdapter
import com.kelsos.mbrc.features.library.tracks.TrackRepository
import com.kelsos.mbrc.features.library.tracks.TrackRepositoryImpl
import com.kelsos.mbrc.features.lyrics.LyricsActivity
import com.kelsos.mbrc.features.lyrics.LyricsViewModel
import com.kelsos.mbrc.features.minicontrol.MiniControlFragment
import com.kelsos.mbrc.features.minicontrol.MiniControlViewModel
import com.kelsos.mbrc.features.nowplaying.MoveManager
import com.kelsos.mbrc.features.nowplaying.MoveManagerImpl
import com.kelsos.mbrc.features.nowplaying.NowPlayingActivity
import com.kelsos.mbrc.features.nowplaying.NowPlayingAdapter
import com.kelsos.mbrc.features.nowplaying.NowPlayingRepository
import com.kelsos.mbrc.features.nowplaying.NowPlayingRepositoryImpl
import com.kelsos.mbrc.features.nowplaying.NowPlayingViewModel
import com.kelsos.mbrc.features.output.OutputApi
import com.kelsos.mbrc.features.output.OutputApiImpl
import com.kelsos.mbrc.features.output.OutputSelectionDialog
import com.kelsos.mbrc.features.output.OutputSelectionViewModel
import com.kelsos.mbrc.features.player.PlayerActivity
import com.kelsos.mbrc.features.player.PlayerViewModel
import com.kelsos.mbrc.features.player.RatingDialogFragment
import com.kelsos.mbrc.features.player.RatingDialogViewModel
import com.kelsos.mbrc.features.playlists.PlaylistActivity
import com.kelsos.mbrc.features.playlists.PlaylistAdapter
import com.kelsos.mbrc.features.playlists.PlaylistRepository
import com.kelsos.mbrc.features.playlists.PlaylistRepositoryImpl
import com.kelsos.mbrc.features.playlists.PlaylistViewModel
import com.kelsos.mbrc.features.queue.QueueHandler
import com.kelsos.mbrc.features.radio.RadioActivity
import com.kelsos.mbrc.features.radio.RadioAdapter
import com.kelsos.mbrc.features.radio.RadioRepository
import com.kelsos.mbrc.features.radio.RadioRepositoryImpl
import com.kelsos.mbrc.features.radio.RadioViewModel
import com.kelsos.mbrc.features.settings.ClientInformationStore
import com.kelsos.mbrc.features.settings.ClientInformationStoreImpl
import com.kelsos.mbrc.features.settings.ConnectionManagerViewModel
import com.kelsos.mbrc.features.settings.ConnectionRepository
import com.kelsos.mbrc.features.settings.ConnectionRepositoryImpl
import com.kelsos.mbrc.features.settings.SettingsManager
import com.kelsos.mbrc.features.settings.SettingsManagerDataStore
import com.kelsos.mbrc.features.theme.ThemeManager
import com.kelsos.mbrc.features.theme.ThemeManagerImpl
import com.kelsos.mbrc.features.widgets.WidgetUpdater
import com.kelsos.mbrc.features.widgets.WidgetUpdaterImpl
import com.kelsos.mbrc.logging.LogHelper
import com.kelsos.mbrc.logging.LogHelperImpl
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.ClientConnectionUseCase
import com.kelsos.mbrc.networking.ClientConnectionUseCaseImpl
import com.kelsos.mbrc.networking.RequestManager
import com.kelsos.mbrc.networking.RequestManagerImpl
import com.kelsos.mbrc.networking.SocketActivityChecker
import com.kelsos.mbrc.networking.client.ClientConnectionManager
import com.kelsos.mbrc.networking.client.ClientConnectionManagerImpl
import com.kelsos.mbrc.networking.client.MessageHandler
import com.kelsos.mbrc.networking.client.MessageHandlerImpl
import com.kelsos.mbrc.networking.client.MessageQueue
import com.kelsos.mbrc.networking.client.MessageQueueImpl
import com.kelsos.mbrc.networking.client.PluginUpdateCheckUseCase
import com.kelsos.mbrc.networking.client.PluginUpdateCheckUseCaseImpl
import com.kelsos.mbrc.networking.client.UiMessageQueue
import com.kelsos.mbrc.networking.client.UiMessageQueueImpl
import com.kelsos.mbrc.networking.discovery.RemoteServiceDiscovery
import com.kelsos.mbrc.networking.discovery.RemoteServiceDiscoveryImpl
import com.kelsos.mbrc.networking.protocol.CommandFactory
import com.kelsos.mbrc.networking.protocol.CommandFactoryImpl
import com.kelsos.mbrc.networking.protocol.ProtocolPingHandle
import com.kelsos.mbrc.networking.protocol.ProtocolVersionUpdate
import com.kelsos.mbrc.networking.protocol.SimpleLogCommand
import com.kelsos.mbrc.networking.protocol.UpdateCover
import com.kelsos.mbrc.networking.protocol.UpdateLastFm
import com.kelsos.mbrc.networking.protocol.UpdateLfmRating
import com.kelsos.mbrc.networking.protocol.UpdateLyrics
import com.kelsos.mbrc.networking.protocol.UpdateMute
import com.kelsos.mbrc.networking.protocol.UpdateNowPlayingList
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
import com.kelsos.mbrc.networking.protocol.UserActionUseCase
import com.kelsos.mbrc.networking.protocol.UserActionUseCaseImpl
import com.kelsos.mbrc.networking.protocol.VolumeModifyUseCase
import com.kelsos.mbrc.networking.protocol.VolumeModifyUseCaseImpl
import com.kelsos.mbrc.platform.RemoteBroadcastReceiver
import com.kelsos.mbrc.platform.ServiceChecker
import com.kelsos.mbrc.platform.ServiceCheckerImpl
import com.kelsos.mbrc.platform.mediasession.AppNotificationManager
import com.kelsos.mbrc.platform.mediasession.AppNotificationManagerImpl
import com.kelsos.mbrc.platform.mediasession.MediaSessionManager
import com.kelsos.mbrc.platform.mediasession.NotificationActionManager
import com.kelsos.mbrc.platform.mediasession.NotificationBuilder
import com.kelsos.mbrc.platform.mediasession.NotificationChannelManager
import com.squareup.moshi.Moshi
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private fun createDispatcher(name: String, threads: Int = 1): ExecutorCoroutineDispatcher {
  var threadId = 1

  return Executors
    .newFixedThreadPool(threads) { runnable ->
      val threadName = if (threads ==
        1
      ) {
        "${name}Dispatcher"
      } else {
        "${name}Dispatcher-worker-${threadId++}"
      }
      Thread(runnable, threadName)
    }.asCoroutineDispatcher()
}

val appModule =
  module {
    single { Moshi.Builder().build() }
    singleOf(::LibrarySyncUseCaseImpl) { bind<LibrarySyncUseCase>() }
    singleOf(::ApiBase)
    singleOf(::RequestManagerImpl) { bind<RequestManager>() }
    singleOf(::OutputApiImpl) { bind<OutputApi>() }

    singleOf(::GenreRepositoryImpl) { bind<GenreRepository>() }
    singleOf(::ArtistRepositoryImpl) { bind<ArtistRepository>() }
    singleOf(::AlbumRepositoryImpl) { bind<AlbumRepository>() }
    singleOf(::TrackRepositoryImpl) { bind<TrackRepository>() }

    singleOf(::NowPlayingRepositoryImpl) { bind<NowPlayingRepository>() }
    singleOf(::PlaylistRepositoryImpl) { bind<PlaylistRepository>() }
    singleOf(::RadioRepositoryImpl) { bind<RadioRepository>() }
    singleOf(::ConnectionRepositoryImpl) { bind<ConnectionRepository>() }

    singleOf(::GenreEntryAdapter)
    singleOf(::ArtistEntryAdapter)

    singleOf(::QueueHandler)

    single<NotificationManagerCompat> { NotificationManagerCompat.from(get()) }
    single { WorkManager.getInstance(get()) }

    singleOf(::RemoteBroadcastReceiver)
    singleOf(::SettingsManagerDataStore) { bind<SettingsManager>() }
    singleOf(::ThemeManagerImpl) { bind<ThemeManager>() }
    singleOf(::ServiceCheckerImpl) { bind<ServiceChecker>() }
    singleOf(::PlayingTrackCacheImpl) { bind<PlayingTrackCache>() }
    singleOf(::SocketActivityChecker)
    singleOf(::CoverCache)
    singleOf(::AppNotificationManagerImpl) { bind<AppNotificationManager>() }

    singleOf(::MediaSessionManager)
    singleOf(::NotificationActionManager)
    singleOf(::NotificationBuilder)
    singleOf(::NotificationChannelManager)
    singleOf(::CommandFactoryImpl) { bind<CommandFactory>() }
    singleOf(::UserActionUseCaseImpl) { bind<UserActionUseCase>() }
    singleOf(::VolumeModifyUseCaseImpl) { bind<VolumeModifyUseCase>() }
    singleOf(::AppStateManager)
    singleOf(::AppState) {
      bind<AppStateFlow>()
      bind<AppStatePublisher>()
    }
    singleOf(::ClientConnectionManagerImpl) { bind<ClientConnectionManager>() }
    singleOf(::ClientInformationStoreImpl) { bind<ClientInformationStore>() }
    singleOf(::ClientConnectionUseCaseImpl) { bind<ClientConnectionUseCase>() }
    singleOf(::MessageHandlerImpl) { bind<MessageHandler>() }
    singleOf(::MessageQueueImpl) { bind<MessageQueue>() }
    singleOf(::UiMessageQueueImpl) { bind<UiMessageQueue>() }
    singleOf(::ConnectionState) {
      bind<ConnectionStateFlow>()
      bind<ConnectionStatePublisher>()
    }
    singleOf(::SerializationAdapterImpl) { bind<SerializationAdapter>() }
    singleOf(::DeserializationAdapterImpl) { bind<DeserializationAdapter>() }
    singleOf(::PluginUpdateCheckUseCaseImpl) { bind<PluginUpdateCheckUseCase>() }

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
    factoryOf(::UpdatePlaybackPositionCommand)
    factoryOf(::UpdatePluginVersionCommand)
    factoryOf(::ProtocolPingHandle)
    factoryOf(::SimpleLogCommand)
    factoryOf(::ProtocolVersionUpdate)

    workerOf(::LibrarySyncWorker)

    singleOf(::RemoteServiceDiscoveryImpl) { bind<RemoteServiceDiscovery>() }
    singleOf(::WidgetUpdaterImpl) { bind<WidgetUpdater>() }
    singleOf(::LibrarySyncWorkHandlerImpl) { bind<LibrarySyncWorkHandler>() }

    val network = createDispatcher(name = "Network", threads = 2)
    val database = createDispatcher(name = "Database")

    single<AppCoroutineDispatchers> {
      @Suppress("InjectDispatcher")
      object : AppCoroutineDispatchers {
        override val main: CoroutineDispatcher = Dispatchers.Main
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val database: CoroutineDispatcher = database
        override val network: CoroutineDispatcher = network
      }
    }

    single {
      Room
        .databaseBuilder(get(), Database::class.java, Database.NAME)
        .addMigrations(MIGRATION_3_4)
        .build()
    }
    singleOf(::DefaultConnectionMigration)
    single { get<Database>().genreDao() }
    single { get<Database>().artistDao() }
    single { get<Database>().albumDao() }
    single { get<Database>().trackDao() }
    single { get<Database>().nowPlayingDao() }
    single { get<Database>().playlistDao() }
    single { get<Database>().radioStationDao() }
    single { get<Database>().connectionDao() }

    singleOf(::MigrationManager)

    scope<MiniControlFragment> {
      viewModelOf(::MiniControlViewModel)
    }

    scope<PlayerActivity> {
      viewModelOf(::PlayerViewModel)
    }

    scope<LyricsActivity> {
      viewModelOf(::LyricsViewModel)
    }

    scope<LibraryActivity> {
      viewModelOf(::LibraryViewModel)
      scopedOf(::LibrarySearchModel)
    }

    scope<BrowseGenreFragment> {
      viewModelOf(::BrowseGenreViewModel)
      scopedOf(::GenreEntryAdapter)
    }

    scope<BrowseArtistFragment> {
      viewModelOf(::BrowseArtistViewModel)
      scopedOf(::ArtistEntryAdapter)
    }

    scope<BrowseAlbumFragment> {
      viewModelOf(::BrowseAlbumViewModel)
      scopedOf(::AlbumEntryAdapter)
    }

    scope<BrowseTrackFragment> {
      viewModelOf(::BrowseTrackViewModel)
      scopedOf(::TrackEntryAdapter)
    }

    scope<GenreArtistsActivity> {
      viewModelOf(::GenreArtistsViewModel)
      scopedOf(::ArtistEntryAdapter)
    }

    scope<ArtistAlbumsActivity> {
      viewModelOf(::ArtistAlbumsViewModel)
      scopedOf(::AlbumEntryAdapter)
    }

    scope<AlbumTracksActivity> {
      viewModelOf(::AlbumTracksViewModel)
      scopedOf(::TrackEntryAdapter)
    }

    scope<NowPlayingActivity> {
      viewModelOf(::NowPlayingViewModel)
      scopedOf(::NowPlayingAdapter)
      scopedOf(::MoveManagerImpl) { bind<MoveManager>() }
    }

    scope<PlaylistActivity> {
      viewModelOf(::PlaylistViewModel)
      scopedOf(::PlaylistAdapter)
    }

    scope<RadioActivity> {
      viewModelOf(::RadioViewModel)
      scopedOf(::RadioAdapter)
    }

    scope<RatingDialogFragment> {
      viewModelOf(::RatingDialogViewModel)
    }

    scope<OutputSelectionDialog> {
      viewModelOf(::OutputSelectionViewModel)
    }

    scope<FeedbackFragment> {
      viewModelOf(::FeedbackViewModel)
      scopedOf(::LogHelperImpl) { bind<LogHelper>() }
    }

    // Global ViewModels
    singleOf(::DrawerViewModel)
    singleOf(::ConnectionManagerViewModel)
  }
