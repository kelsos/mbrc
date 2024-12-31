package com.kelsos.mbrc

import androidx.core.app.NotificationManagerCompat
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.di.providers.NotificationManagerCompatProvider
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.bus.RxBusImpl
import com.kelsos.mbrc.features.library.AlbumRepository
import com.kelsos.mbrc.features.library.AlbumRepositoryImpl
import com.kelsos.mbrc.features.library.ArtistRepository
import com.kelsos.mbrc.features.library.ArtistRepositoryImpl
import com.kelsos.mbrc.features.library.GenreRepository
import com.kelsos.mbrc.features.library.GenreRepositoryImpl
import com.kelsos.mbrc.features.library.LibrarySyncInteractor
import com.kelsos.mbrc.features.library.LibrarySyncInteractorImpl
import com.kelsos.mbrc.features.library.LocalArtistDataSource
import com.kelsos.mbrc.features.library.LocalArtistDataSourceImpl
import com.kelsos.mbrc.features.library.TrackRepository
import com.kelsos.mbrc.features.library.TrackRepositoryImpl
import com.kelsos.mbrc.features.nowplaying.NowPlayingRepository
import com.kelsos.mbrc.features.nowplaying.NowPlayingRepositoryImpl
import com.kelsos.mbrc.features.output.OutputApi
import com.kelsos.mbrc.features.output.OutputApiImpl
import com.kelsos.mbrc.features.player.ModelCache
import com.kelsos.mbrc.features.player.ModelCacheImpl
import com.kelsos.mbrc.features.playlists.PlaylistRepository
import com.kelsos.mbrc.features.playlists.PlaylistRepositoryImpl
import com.kelsos.mbrc.features.queue.QueueHandler
import com.kelsos.mbrc.features.radio.LocalRadioDataSource
import com.kelsos.mbrc.features.radio.RadioRepository
import com.kelsos.mbrc.features.radio.RadioRepositoryImpl
import com.kelsos.mbrc.features.radio.RemoteRadioDataSource
import com.kelsos.mbrc.features.settings.ConnectionRepository
import com.kelsos.mbrc.features.settings.ConnectionRepositoryImpl
import com.kelsos.mbrc.features.settings.SettingsManager
import com.kelsos.mbrc.features.settings.SettingsManagerImpl
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.RequestManager
import com.kelsos.mbrc.networking.RequestManagerImpl
import com.kelsos.mbrc.platform.ServiceChecker
import com.kelsos.mbrc.platform.ServiceCheckerImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import toothpick.config.Module
import java.util.concurrent.Executors

class AppModule : Module() {
  init {
    val mapper = ObjectMapper().registerKotlinModule()

    bind(RxBus::class.java).to(RxBusImpl::class.java).singletonInScope()
    bind(ObjectMapper::class.java).toInstance(mapper)

    bind(NotificationManagerCompat::class.java).toProvider(NotificationManagerCompatProvider::class.java)
    bind(ConnectionRepository::class.java).to(ConnectionRepositoryImpl::class.java)
    bind(Scheduler::class.java)
      .withName("main")
      .toProviderInstance { AndroidSchedulers.mainThread() }
    bind(Scheduler::class.java).withName("io").toProviderInstance { Schedulers.io() }

    bind(TrackRepository::class.java).to(TrackRepositoryImpl::class.java)
    bind(AlbumRepository::class.java).to(AlbumRepositoryImpl::class.java)
    bind(ArtistRepository::class.java).to(ArtistRepositoryImpl::class.java)
    bind(GenreRepository::class.java).to(GenreRepositoryImpl::class.java)

    bind(LocalArtistDataSource::class.java).to(LocalArtistDataSourceImpl::class.java)

    bind(NowPlayingRepository::class.java).to(NowPlayingRepositoryImpl::class.java)
    bind(PlaylistRepository::class.java).to(PlaylistRepositoryImpl::class.java)

    bind(SettingsManager::class.java).to(SettingsManagerImpl::class.java).singletonInScope()
    bind(ModelCache::class.java).to(ModelCacheImpl::class.java).singletonInScope()
    bind(ServiceChecker::class.java).to(ServiceCheckerImpl::class.java).singletonInScope()

    bind(LibrarySyncInteractor::class.java)
      .to(LibrarySyncInteractorImpl::class.java)
      .singletonInScope()
    bind(ApiBase::class.java).singletonInScope()
    bind(RequestManager::class.java).to(RequestManagerImpl::class.java).singletonInScope()
    bind(OutputApi::class.java).to(OutputApiImpl::class.java).singletonInScope()
    bind(AppCoroutineDispatchers::class.java).toInstance(
      AppCoroutineDispatchers(
        Dispatchers.Main,
        Dispatchers.IO,
        Executors.newSingleThreadExecutor { Thread(it, "db") }.asCoroutineDispatcher(),
      ),
    )
    bind(QueueHandler::class.java).singletonInScope()

    bind(LocalRadioDataSource::class.java).to(LocalRadioDataSource::class.java).singletonInScope()
    bind(RemoteRadioDataSource::class.java).to(RemoteRadioDataSource::class.java).singletonInScope()
    bind(RadioRepository::class.java).to(RadioRepositoryImpl::class.java).singletonInScope()
  }
}
