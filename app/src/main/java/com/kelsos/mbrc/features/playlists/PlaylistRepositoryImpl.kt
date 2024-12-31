package com.kelsos.mbrc.features.playlists

import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.raizlabs.android.dbflow.list.FlowCursorList
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class PlaylistRepositoryImpl
  @Inject
  constructor(
    private val localDataSource: LocalPlaylistDataSource,
    private val remoteDataSource: RemotePlaylistDataSource,
    private val dispatchers: AppCoroutineDispatchers,
  ) : PlaylistRepository {
    override suspend fun getAllCursor(): FlowCursorList<Playlist> = localDataSource.loadAllCursor()

    override suspend fun getAndSaveRemote(): FlowCursorList<Playlist> {
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
          }.collect { list ->
            val data = list.map { it.apply { dateAdded = epoch } }
            localDataSource.saveAll(data)
          }
      }
    }

    override suspend fun search(term: String): FlowCursorList<Playlist> = localDataSource.search(term)

    override suspend fun cacheIsEmpty(): Boolean = localDataSource.isEmpty()

    override suspend fun count(): Long = localDataSource.count()
  }
