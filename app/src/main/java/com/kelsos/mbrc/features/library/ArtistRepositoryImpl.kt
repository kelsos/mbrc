package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.raizlabs.android.dbflow.list.FlowCursorList
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class ArtistRepositoryImpl
  @Inject
  constructor(
    private val localDataSource: LocalArtistDataSource,
    private val remoteDataSource: RemoteArtistDataSource,
    private val dispatchers: AppCoroutineDispatchers,
  ) : ArtistRepository {
    override suspend fun getArtistByGenre(genre: String): FlowCursorList<Artist> = localDataSource.getArtistByGenre(genre)

    override suspend fun getAllCursor(): FlowCursorList<Artist> = localDataSource.loadAllCursor()

    override suspend fun getAndSaveRemote(): FlowCursorList<Artist> {
      getRemote()
      return localDataSource.loadAllCursor()
    }

    override suspend fun getRemote() {
      val epoch = Instant.now().epochSecond
      withContext(dispatchers.io) {
        remoteDataSource
          .fetch()
          .onCompletion {
            localDataSource.removePreviousEntries(epoch)
          }.collect { artists ->
            val data = artists.map { it.apply { dateAdded = epoch } }
            localDataSource.saveAll(data)
          }
      }
    }

    override suspend fun search(term: String): FlowCursorList<Artist> = localDataSource.search(term)

    override suspend fun getAlbumArtistsOnly(): FlowCursorList<Artist> = localDataSource.getAlbumArtists()

    override suspend fun getAllRemoteAndShowAlbumArtist(): FlowCursorList<Artist> {
      getRemote()
      return localDataSource.getAlbumArtists()
    }

    override suspend fun cacheIsEmpty(): Boolean = localDataSource.isEmpty()

    override suspend fun count(): Long = localDataSource.count()
  }
