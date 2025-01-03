package com.kelsos.mbrc.features.library

import android.app.Application
import com.kelsos.mbrc.common.data.Progress
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.features.library.albums.AlbumCover
import com.kelsos.mbrc.features.library.albums.AlbumRepository
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.ApiStatus
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.decodeBase64
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.File

class CoverCache(
  private val albumRepository: AlbumRepository,
  private val api: ApiBase,
  private val dispatchers: AppCoroutineDispatchers,
  app: Application,
) {
  private val cache = File(app.cacheDir, "covers")

  init {
    if (!cache.exists()) {
      val isSuccess = cache.mkdir()
      Timber.v("Cover cache directory ${cache.absolutePath} created: $isSuccess")
    }
  }

  suspend fun cache(progress: Progress? = null) {
    val covers = getCachedCovers()
    fetchCovers(covers, progress)
  }

  private suspend fun getCachedCovers(): List<AlbumCover> =
    withContext(dispatchers.database) {
      val covers = albumRepository.getCovers()
      val albumCovers = mutableListOf<AlbumCover>()
      withContext(dispatchers.io) {
        val files = cache.listFiles()?.map { it.nameWithoutExtension }.orEmpty()

        for (cover in covers) {
          if (cover.hash.isNullOrEmpty() || files.contains(cover.key())) {
            albumCovers.add(cover)
          } else {
            albumCovers.add(cover.copy(hash = null))
          }
        }
      }
      albumCovers
    }

  private suspend fun fetchCovers(
    covers: List<AlbumCover>,
    progress: Progress?,
  ) {
    withContext(dispatchers.network) {
      val updated = mutableListOf<AlbumCover>()
      api
        .getAll(Protocol.LibraryCover, covers, Cover::class, progress)
        .onCompletion {
          finalizeUpdate(updated)
        }.collect { (payload, response) ->
          processApiCoverResponse(response, payload, updated)
        }
    }
  }

  private fun processApiCoverResponse(
    response: Cover,
    payload: AlbumCover,
    updated: MutableList<AlbumCover>,
  ) {
    val status = response.status
    if (status == ApiStatus.NOT_MODIFIED) {
      Timber.v("cover for $payload did not change")
      return
    }

    val cover = response.cover
    val hash = response.hash

    if (status == ApiStatus.SUCCESS && !cover.isNullOrEmpty() && !hash.isNullOrEmpty()) {
      val result = cacheAlbumCover(payload, cover, updated, hash)

      if (result.isFailure) {
        Timber.e(result.exceptionOrNull(), "Could not save cover for $payload")
      }
    }
  }

  private suspend fun finalizeUpdate(updated: List<AlbumCover>) {
    Timber.v("Updated covers for ${updated.size} albums")
    withContext(dispatchers.database) {
      albumRepository.updateCovers(updated)
    }
    val storedCovers = albumRepository.getCovers().map { it.key() }
    val coverFiles = cache.listFiles()
    if (coverFiles != null) {
      val notInDb = coverFiles.filter { !storedCovers.contains(it.nameWithoutExtension) }
      Timber.v("deleting ${notInDb.size} covers no longer in db")
      for (file in notInDb) {
        runCatching { file.delete() }
      }
    }
  }

  private fun cacheAlbumCover(
    payload: AlbumCover,
    cover: String,
    updated: MutableList<AlbumCover>,
    hash: String,
  ): Result<Boolean> =
    runCatching {
      val file = File(cache, payload.key())
      val decodeBase64 = cover.decodeBase64()
      if (decodeBase64 != null) {
        Timber.v("saving cover for $payload -> ${file.path}")
        file.sink().buffer().use { sink -> sink.write(decodeBase64) }
      }
      updated.add(payload.copy(hash = hash))
    }
}
