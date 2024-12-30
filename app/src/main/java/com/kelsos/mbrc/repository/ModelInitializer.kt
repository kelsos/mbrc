package com.kelsos.mbrc.repository


import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.domain.isEmpty
import com.kelsos.mbrc.model.MainDataModel
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelInitializer
@Inject
constructor(
  private val model: MainDataModel,
  private val cache: ModelCache,
  private val dispatchers: AppDispatchers
) {
  private var done = false
  suspend fun initialize() {
    if (done) {
      return
    }

    done = true
    try {
      withContext(dispatchers.io) {
        if (model.trackInfo.isEmpty()) {
          model.trackInfo = cache.restoreInfo()
          Timber.v("Loaded trackinfo")
        }

        if (model.coverPath.isEmpty()) {
          model.coverPath = cache.restoreCover()
          Timber.v("Loaded cover")
        }
      }
    } catch (e: Exception) {
      onLoadError(e)
      done = false
    }
  }

  private fun onLoadError(it: Throwable?) {
    if (it is FileNotFoundException) {
      Timber.v("No state was previously stored")
    } else {
      Timber.v(it, "Error while loading the state")
    }
  }
}


