package com.kelsos.mbrc.feature.library.tracks

import androidx.paging.PagingData
import com.kelsos.mbrc.core.common.data.Progress
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.common.settings.TrackSortField
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.common.utilities.epoch
import com.kelsos.mbrc.core.data.library.track.PagingTrackQuery
import com.kelsos.mbrc.core.data.library.track.Track
import com.kelsos.mbrc.core.data.library.track.TrackDao
import com.kelsos.mbrc.core.data.library.track.TrackQuery
import com.kelsos.mbrc.core.data.library.track.TrackRepository
import com.kelsos.mbrc.core.data.paged
import com.kelsos.mbrc.core.networking.api.LibraryApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext

class TrackRepositoryImpl(
  private val dao: TrackDao,
  private val libraryApi: LibraryApi,
  private val dispatchers: AppCoroutineDispatchers
) : TrackRepository {
  override suspend fun count(): Long = withContext(dispatchers.database) { dao.count() }

  override fun getAll(): Flow<PagingData<Track>> =
    paged({ dao.getAll() }, key = { it.id }) { it.toTrack() }

  override fun getTracks(query: PagingTrackQuery): Flow<PagingData<Track>> = when (query) {
    is PagingTrackQuery.Album ->
      paged({
        dao.getAlbumTracks(query.album, query.artist)
      }, key = { it.id }) { it.toTrack() }

    is PagingTrackQuery.NonAlbum ->
      paged(
        { dao.getNonAlbumTracks(query.artist) },
        key = { it.id }
      ) { it.toTrack() }
  }

  override suspend fun getRemote(progress: Progress?) {
    withContext(dispatchers.network) {
      val added = epoch()
      val allPages = libraryApi.getTracks(progress)
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
                .chunked(size = 100)
                .flatMap { dao.findMatchingIds(it) }
                .associate { it.src to it.id }

            val toUpdate = tracks.filter { matches.containsKey(it.src) }
            val toInsert = tracks.minus(toUpdate.toSet())

            dao.update(
              toUpdate.map {
                it.copy(id = matches.getValue(requireNotNull(it.src)))
              }
            )
            dao.insertAll(toInsert)
          }
        }
    }
  }

  override fun search(term: String): Flow<PagingData<Track>> = paged({
    dao.search(term)
  }, key = { it.id }) { it.toTrack() }

  override fun getAll(field: TrackSortField, order: SortOrder): Flow<PagingData<Track>> = paged({
    when (field) {
      TrackSortField.TITLE -> when (order) {
        SortOrder.ASC -> dao.getAllByTitleAsc()
        SortOrder.DESC -> dao.getAllByTitleDesc()
      }

      TrackSortField.ARTIST -> when (order) {
        SortOrder.ASC -> dao.getAllByArtistAsc()
        SortOrder.DESC -> dao.getAllByArtistDesc()
      }

      TrackSortField.ALBUM -> when (order) {
        SortOrder.ASC -> dao.getAllByAlbumAsc()
        SortOrder.DESC -> dao.getAllByAlbumDesc()
      }

      TrackSortField.ALBUM_ARTIST -> when (order) {
        SortOrder.ASC -> dao.getAllByAlbumArtistAsc()
        SortOrder.DESC -> dao.getAllByAlbumArtistDesc()
      }
    }
  }, key = { it.id }) { it.toTrack() }

  override fun search(
    term: String,
    field: TrackSortField,
    order: SortOrder
  ): Flow<PagingData<Track>> = paged({
    when (field) {
      TrackSortField.TITLE -> when (order) {
        SortOrder.ASC -> dao.searchByTitleAsc(term)
        SortOrder.DESC -> dao.searchByTitleDesc(term)
      }

      TrackSortField.ARTIST -> when (order) {
        SortOrder.ASC -> dao.searchByArtistAsc(term)
        SortOrder.DESC -> dao.searchByArtistDesc(term)
      }

      TrackSortField.ALBUM -> when (order) {
        SortOrder.ASC -> dao.searchByAlbumAsc(term)
        SortOrder.DESC -> dao.searchByAlbumDesc(term)
      }

      TrackSortField.ALBUM_ARTIST -> when (order) {
        SortOrder.ASC -> dao.searchByAlbumArtistAsc(term)
        SortOrder.DESC -> dao.searchByAlbumArtistDesc(term)
      }
    }
  }, key = { it.id }) { it.toTrack() }

  override fun getTrackPaths(query: TrackQuery): List<String> = when (query) {
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

  override suspend fun getByPath(path: String): Track? = withContext(dispatchers.database) {
    dao.getByPath(path)?.toTrack()
  }
}
