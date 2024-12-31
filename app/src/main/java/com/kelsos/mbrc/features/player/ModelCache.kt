package com.kelsos.mbrc.features.player

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.store.Store
import com.kelsos.mbrc.store.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

internal val Context.cacheDataStore: DataStore<Store> by dataStore(
  fileName = "cache_store.db",
  serializer = PlayerStateSerializer,
)

class ModelCacheImpl
  @Inject
  constructor(
    private val context: Application,
    private val dispatchers: AppCoroutineDispatchers,
  ) : ModelCache {
    private val storeFlow: Flow<Store> =
      context.cacheDataStore.data
        .catch { exception ->
          // dataStore.data throws an IOException when an error is encountered when reading data
          if (exception is IOException) {
            Timber.Forest.e(exception, "Error reading sort order preferences.")
            emit(Store.getDefaultInstance())
          } else {
            throw exception
          }
        }

    override suspend fun persistInfo(trackInfo: TrackInfo) =
      withContext(dispatchers.io) {
        context.cacheDataStore.updateData { store ->
          val track =
            Track
              .newBuilder()
              .setAlbum(trackInfo.album)
              .setArtist(trackInfo.artist)
              .setPath(trackInfo.path)
              .setTitle(trackInfo.title)
              .setYear(trackInfo.year)
              .build()

          store
            .toBuilder()
            .setTrack(track)
            .build()
        }
        return@withContext
      }

    override suspend fun restoreInfo(): TrackInfo =
      withContext(dispatchers.io) {
        val track = storeFlow.first().track

        return@withContext TrackInfo(
          track.artist,
          track.title,
          track.album,
          track.year,
          track.path,
        )
      }

    override suspend fun persistCover(cover: String) {
      context.cacheDataStore.updateData { store ->
        store.toBuilder().setCover(cover).build()
      }
    }

    override suspend fun restoreCover(): String = storeFlow.first().cover
  }

interface ModelCache {
  suspend fun persistInfo(trackInfo: TrackInfo)

  suspend fun restoreInfo(): TrackInfo

  suspend fun persistCover(cover: String)

  suspend fun restoreCover(): String
}
