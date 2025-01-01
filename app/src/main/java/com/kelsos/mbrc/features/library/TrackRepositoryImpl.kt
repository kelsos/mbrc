package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.raizlabs.android.dbflow.list.FlowCursorList
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import java.time.Instant

class TrackRepositoryImpl(
  private val localDataSource: LocalTrackDataSource,
  private val remoteDataSource: RemoteTrackDataSource,
  private val dispatchers: AppCoroutineDispatchers,
) : TrackRepository {
  override suspend fun getAllCursor(): FlowCursorList<Track> = localDataSource.loadAllCursor()

  override suspend fun getAlbumTracks(
    album: String,
    artist: String,
  ): FlowCursorList<Track> = localDataSource.getAlbumTracks(album, artist)

  override suspend fun getNonAlbumTracks(artist: String): FlowCursorList<Track> = localDataSource.getNonAlbumTracks(artist)

  override suspend fun getAndSaveRemote(): FlowCursorList<Track> {
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
        }.collect { tracks ->
          val data =
            tracks.map {
              it.apply {
                dateAdded = epoch
              }
            }
          localDataSource.saveAll(data)
        }
    }
  }

  override suspend fun search(term: String): FlowCursorList<Track> = localDataSource.search(term)

  override suspend fun getGenreTrackPaths(genre: String): List<String> = localDataSource.getGenreTrackPaths(genre)

  override suspend fun getArtistTrackPaths(artist: String): List<String> = localDataSource.getArtistTrackPaths(artist)

  override suspend fun getAlbumTrackPaths(
    album: String,
    artist: String,
  ): List<String> = localDataSource.getAlbumTrackPaths(album, artist)

  override suspend fun getAllTrackPaths(): List<String> = localDataSource.getAllTrackPaths()

  override suspend fun cacheIsEmpty(): Boolean = localDataSource.isEmpty()

  override suspend fun count(): Long = localDataSource.count()
}
