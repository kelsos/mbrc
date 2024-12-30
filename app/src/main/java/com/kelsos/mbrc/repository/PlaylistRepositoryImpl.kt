package com.kelsos.mbrc.repository

import com.kelsos.mbrc.data.Playlist
import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.repository.data.LocalPlaylistDataSource
import com.kelsos.mbrc.repository.data.RemotePlaylistDataSource
import com.raizlabs.android.dbflow.list.FlowCursorList
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import org.threeten.bp.Instant
import javax.inject.Inject

class PlaylistRepositoryImpl
@Inject constructor(
  private val localDataSource: LocalPlaylistDataSource,
  private val remoteDataSource: RemotePlaylistDataSource,
  private val dispatchers: AppDispatchers
) : PlaylistRepository {
  override suspend fun getAllCursor(): FlowCursorList<Playlist> = localDataSource.loadAllCursor()

  override suspend fun getAndSaveRemote(): FlowCursorList<Playlist> {
    getRemote()
    return localDataSource.loadAllCursor()
  }

  override suspend fun getRemote() {
    val epoch = Instant.now().epochSecond
    withContext(dispatchers.io) {
      remoteDataSource.fetch()
        .onCompletion {
          localDataSource.removePreviousEntries(epoch)
        }
        .collect { list ->
          val data = list.map { it.apply { dateAdded = epoch } }
          localDataSource.saveAll(data)
        }
    }
  }

  override suspend fun search(term: String): FlowCursorList<Playlist> = localDataSource.search(term)

  override suspend fun cacheIsEmpty(): Boolean = localDataSource.isEmpty()

  override suspend fun count(): Long = localDataSource.count()
}
