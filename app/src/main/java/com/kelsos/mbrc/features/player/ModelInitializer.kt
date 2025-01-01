package com.kelsos.mbrc.features.player

import com.kelsos.mbrc.common.state.MainDataModel
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileNotFoundException

class ModelInitializer(
  private val model: MainDataModel,
  private val cache: ModelCache,
  private val dispatchers: AppCoroutineDispatchers,
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
          Timber.Forest.v("Loaded trackinfo")
        }

        if (model.coverPath.isEmpty()) {
          model.coverPath = cache.restoreCover()
          Timber.Forest.v("Loaded cover")
        }
      }
    } catch (e: Exception) {
      onLoadError(e)
      done = false
    }
  }

  private fun onLoadError(it: Throwable?) {
    if (it is FileNotFoundException) {
      Timber.Forest.v("No state was previously stored")
    } else {
      Timber.Forest.v(it, "Error while loading the state")
    }
  }
}
