package com.kelsos.mbrc.feature.library

import com.kelsos.mbrc.core.data.library.album.AlbumRepository
import com.kelsos.mbrc.core.data.library.artist.ArtistRepository
import com.kelsos.mbrc.core.data.library.genre.GenreRepository
import com.kelsos.mbrc.core.data.library.track.TrackRepository
import com.kelsos.mbrc.core.queue.PathQueueUseCase
import com.kelsos.mbrc.feature.library.albums.AlbumRepositoryImpl
import com.kelsos.mbrc.feature.library.albums.ArtistAlbumsViewModel
import com.kelsos.mbrc.feature.library.albums.BrowseAlbumViewModel
import com.kelsos.mbrc.feature.library.albums.GenreAlbumsViewModel
import com.kelsos.mbrc.feature.library.artists.ArtistRepositoryImpl
import com.kelsos.mbrc.feature.library.artists.BrowseArtistViewModel
import com.kelsos.mbrc.feature.library.artists.GenreArtistsViewModel
import com.kelsos.mbrc.feature.library.data.CoverCache
import com.kelsos.mbrc.feature.library.domain.LibraryDerivationUseCase
import com.kelsos.mbrc.feature.library.domain.LibrarySyncUseCase
import com.kelsos.mbrc.feature.library.domain.LibrarySyncUseCaseImpl
import com.kelsos.mbrc.feature.library.domain.LibrarySyncWorkHandler
import com.kelsos.mbrc.feature.library.domain.LibrarySyncWorkHandlerImpl
import com.kelsos.mbrc.feature.library.domain.LibrarySyncWorker
import com.kelsos.mbrc.feature.library.genres.BrowseGenreViewModel
import com.kelsos.mbrc.feature.library.genres.GenreRepositoryImpl
import com.kelsos.mbrc.feature.library.queue.QueueHandler
import com.kelsos.mbrc.feature.library.tracks.AlbumTracksViewModel
import com.kelsos.mbrc.feature.library.tracks.BrowseTrackViewModel
import com.kelsos.mbrc.feature.library.tracks.TrackRepositoryImpl
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for library feature dependencies.
 *
 * This module provides:
 * - Library repositories (Genre, Artist, Album, Track)
 * - Library sync use case and worker
 * - Cover cache
 * - Queue handler
 * - All library-related ViewModels
 *
 * Required dependencies from other modules:
 * - DAOs from core/data module
 * - APIs from core/networking module
 * - AppCoroutineDispatchers from app module
 */
val libraryModule = module {
  // Repositories
  singleOf(::GenreRepositoryImpl) { bind<GenreRepository>() }
  singleOf(::ArtistRepositoryImpl) { bind<ArtistRepository>() }
  singleOf(::AlbumRepositoryImpl) { bind<AlbumRepository>() }
  singleOf(::TrackRepositoryImpl) { bind<TrackRepository>() }

  // Library sync
  singleOf(::LibraryDerivationUseCase)
  singleOf(::LibrarySyncUseCaseImpl) { bind<LibrarySyncUseCase>() }
  singleOf(::LibrarySyncWorkHandlerImpl) { bind<LibrarySyncWorkHandler>() }
  workerOf(::LibrarySyncWorker)

  // Utilities
  singleOf(::CoverCache)
  singleOf(::QueueHandler) { bind<PathQueueUseCase>() }

  // ViewModels
  singleOf(::LibrarySearchModel)
  viewModelOf(::LibraryViewModel)
  viewModelOf(::BrowseGenreViewModel)
  viewModelOf(::BrowseArtistViewModel)
  viewModelOf(::BrowseAlbumViewModel)
  viewModelOf(::BrowseTrackViewModel)
  viewModelOf(::GenreArtistsViewModel)
  viewModelOf(::GenreAlbumsViewModel)
  viewModelOf(::ArtistAlbumsViewModel)
  viewModelOf(::AlbumTracksViewModel)
}
