package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.raizlabs.android.dbflow.list.FlowCursorList
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class AlbumRepositoryImpl
  @Inject
  constructor(
    private val localDataSource: LocalAlbumDataSource,
    private val remoteDataSource: RemoteAlbumDataSource,
    private val dispatchers: AppCoroutineDispatchers,
  ) : AlbumRepository {
    override suspend fun getAlbumsByArtist(artist: String): FlowCursorList<Album> = localDataSource.getAlbumsByArtist(artist)

    override suspend fun getAllCursor(): FlowCursorList<Album> = localDataSource.loadAllCursor()

    override suspend fun getAndSaveRemote(): FlowCursorList<Album> {
      getRemote()
      return localDataSource.loadAllCursor()
    }

    override suspend fun getRemote() {
      val epoch = Instant.now().epochSecond
      val default = CachedAlbumInfo(0, null)
      val cached =
        localDataSource.loadAllCursor().associate {
          it.album + it.artist to CachedAlbumInfo(it.id, it?.cover)
        }
      withContext(dispatchers.io) {
        remoteDataSource
          .fetch()
          .onCompletion {
            localDataSource.removePreviousEntries(epoch)
          }.collect { albums ->
            val list =
              albums.map {
                it.apply {
                  dateAdded = epoch
                  val key = it.album + it.artist

                  if (cached.containsKey(key)) {
                    val cachedAlbum = cached.getOrDefault(key, default)
                    id = cachedAlbum.id
                    cover = cachedAlbum.cover
                  }
                }
              }
            localDataSource.saveAll(list)
          }
      }
    }

    override suspend fun search(term: String): FlowCursorList<Album> = localDataSource.search(term)

    override suspend fun cacheIsEmpty(): Boolean = localDataSource.isEmpty()

    override suspend fun count(): Long = localDataSource.count()

    override suspend fun updateCovers(updated: List<CoverInfo>) {
      localDataSource.updateCovers(updated)
    }
  }
