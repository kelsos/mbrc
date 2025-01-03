package com.kelsos.mbrc.features.library.tracks

import androidx.paging.PagingData
import com.kelsos.mbrc.common.data.Progress
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.common.utilities.epoch
import com.kelsos.mbrc.common.utilities.paged
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext

class TrackRepositoryImpl(
  private val dao: TrackDao,
  private val api: ApiBase,
  private val dispatchers: AppCoroutineDispatchers,
) : TrackRepository {
  override suspend fun count(): Long = withContext(dispatchers.database) { dao.count() }

  override fun getAll(): Flow<PagingData<Track>> = paged({ dao.getAll() }) { it.toTrack() }

  override fun getTracks(query: PagingTrackQuery): Flow<PagingData<Track>> =
    when (query) {
      is PagingTrackQuery.Album ->
        paged({
          dao.getAlbumTracks(query.album, query.artist)
        }) { it.toTrack() }
      is PagingTrackQuery.NonAlbum ->
        paged(
          { dao.getNonAlbumTracks(query.artist) },
        ) { it.toTrack() }
    }

  override suspend fun getRemote(progress: Progress?) {
    withContext(dispatchers.network) {
      val added = epoch()
      val allPages =
        api.getAllPages(
          Protocol.LibraryBrowseTracks,
          TrackDto::class,
          progress,
        )
      allPages
        .onCompletion {
          withContext(dispatchers.database) {
            dao.removePreviousEntries(added)
          }
        }.collect { items ->
          val tracks = items.map { it.toEntity().copy(dateAdded = added) }
          val sources = tracks.map { requireNotNull(it.src) }

          withContext(dispatchers.database) {
            val matches =
              sources
                .chunked(size = 50)
                .flatMap { dao.findMatchingIds(it) }
                .associate { it.src to it.id }

            val toUpdate = tracks.filter { matches.containsKey(it.src) }
            val toInsert = tracks.minus(toUpdate.toSet())

            dao.update(
              toUpdate.map {
                it.copy(id = matches.getValue(requireNotNull(it.src)))
              },
            )
            dao.insertAll(toInsert)
          }
        }
    }
  }

  override fun search(term: String): Flow<PagingData<Track>> =
    paged({
      dao.search(term)
    }) { it.toTrack() }

  override fun getTrackPaths(query: TrackQuery): List<String> =
    when (query) {
      is TrackQuery.All -> dao.getAllTrackPaths()
      is TrackQuery.Genre -> dao.getGenreTrackPaths(query.genre)
      is TrackQuery.Artist -> dao.getArtistTrackPaths(query.artist)
      is TrackQuery.Album -> dao.getAlbumTrackPaths(query.album, query.artist)
    }

  override suspend fun getById(id: Long): Track? {
    return withContext(dispatchers.database) {
      val entity = dao.getById(id) ?: return@withContext null
      return@withContext entity.toTrack()
    }
  }
}
