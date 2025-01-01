package com.kelsos.mbrc.features.library

import android.app.Application
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.networking.ApiBase
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
      cache.mkdir()
    }
  }

  suspend fun cache() {
    val covers =
      withContext(dispatchers.database) {
        val albumCovers = mutableListOf<CoverInfo>()
        val covers =
          albumRepository
            .getAllCursor()
            .map {
              CoverInfo(
                artist = it.artist ?: "",
                album = it.album ?: "",
                hash = it.cover ?: "",
              )
            }
        withContext(dispatchers.io) {
          val files = cache.listFiles()?.map { it.nameWithoutExtension } ?: emptyList()

          for (cover in covers) {
            if (cover.hash.isBlank() || files.contains(cover.key())) {
              albumCovers.add(cover)
            } else {
              albumCovers.add(cover.copy(hash = ""))
            }
          }
        }
        albumCovers
      }
    withContext(dispatchers.io) {
      val updated = mutableListOf<CoverInfo>()
      api
        .getAll(Protocol.LIBRARY_COVER, covers, Cover::class)
        .onCompletion {
          Timber.Forest.v("Updated covers for ${updated.size} albums")
          withContext(dispatchers.database) {
            albumRepository.updateCovers(updated)
          }
          val storedCovers = albumRepository.getAllCursor().map { it.key() }
          val coverFiles = cache.listFiles()
          if (coverFiles != null) {
            val notInDb = coverFiles.filter { !storedCovers.contains(it.nameWithoutExtension) }
            Timber.Forest.v("deleting ${notInDb.size} covers no longer in db")
            for (file in notInDb) {
              runCatching { file.delete() }
            }
          }
        }.collect { (payload, response) ->
          if (response.status == 304) {
            Timber.Forest.v("cover for $payload did not change")
            return@collect
          }
          val cover = response.cover
          val hash = response.hash

          if (response.status == 200 && !cover.isNullOrEmpty() && !hash.isNullOrEmpty()) {
            val result =
              runCatching {
                val file = File(cache, payload.key())
                val decodeBase64 = cover.decodeBase64()
                if (decodeBase64 != null) {
                  file.sink().buffer().use { sink -> sink.write(decodeBase64) }
                }
                updated.add(payload.copy(hash = hash))
              }

            if (result.isFailure) {
              Timber.Forest.e(result.exceptionOrNull(), "Could not save cover for $payload")
            }
          }
        }
    }
  }
}
