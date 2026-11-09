package com.kelsos.mbrc.feature.content.playlists

import androidx.paging.PagingData
import com.kelsos.mbrc.core.common.data.Progress
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.common.utilities.epoch
import com.kelsos.mbrc.core.data.paged
import com.kelsos.mbrc.core.data.playlist.Playlist
import com.kelsos.mbrc.core.data.playlist.PlaylistBrowserItem
import com.kelsos.mbrc.core.data.playlist.PlaylistDao
import com.kelsos.mbrc.core.data.playlist.PlaylistRepository
import com.kelsos.mbrc.core.networking.api.ContentApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext

class PlaylistRepositoryImpl(
  private val dao: PlaylistDao,
  private val contentApi: ContentApi,
  private val dispatchers: AppCoroutineDispatchers
) : PlaylistRepository {
  override suspend fun count(): Long = withContext(dispatchers.database) { dao.count() }

  override fun getAll(): Flow<PagingData<Playlist>> =
    paged({ dao.getAll() }, key = { it.id }) { it.toPlaylist() }

  override suspend fun getRemote(progress: Progress?) {
    withContext(dispatchers.network) {
      val added = epoch()
      val allPages = contentApi.getPlaylists(progress)
      allPages
        .onCompletion {
          withContext(dispatchers.database) {
            dao.removePreviousEntries(added)
          }
        }.collect { items ->
          val playlists = items.map { it.toEntity().copy(dateAdded = added) }
          withContext(dispatchers.database) {
            dao.insertAll(playlists)
          }
        }
    }
  }

  override fun search(term: String): Flow<PagingData<Playlist>> = paged({
    dao.search(term)
  }, key = { it.id }) { it.toPlaylist() }

  override suspend fun getById(id: Long): Playlist? {
    return withContext(dispatchers.database) {
      val entity = dao.getById(id) ?: return@withContext null
      return@withContext entity.toPlaylist()
    }
  }

  override fun getBrowserItemsAtPath(path: String): Flow<PagingData<PlaylistBrowserItem>> = paged({
    if (path.isEmpty()) {
      dao.getBrowserItemsAtRoot()
    } else {
      dao.getBrowserItemsAtPath(path)
    }
  }, key = { it.key }) { it }
}
