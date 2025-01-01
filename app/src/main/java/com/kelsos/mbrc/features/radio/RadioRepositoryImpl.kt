package com.kelsos.mbrc.features.radio

import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.raizlabs.android.dbflow.list.FlowCursorList
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import java.time.Instant

class RadioRepositoryImpl(
  private val localDataSource: LocalRadioDataSource,
  private val remoteDataSource: RemoteRadioDataSource,
  private val dispatchers: AppCoroutineDispatchers,
) : RadioRepository {
  override suspend fun getAllCursor(): FlowCursorList<RadioStation> = localDataSource.loadAllCursor()

  override suspend fun getAndSaveRemote(): FlowCursorList<RadioStation> {
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
        }.collect { radios ->
          val data = radios.map { it.apply { dateAdded = epoch } }
          localDataSource.saveAll(data)
        }
    }
  }

  override suspend fun search(term: String): FlowCursorList<RadioStation> = localDataSource.search(term)

  override suspend fun cacheIsEmpty(): Boolean = localDataSource.isEmpty()

  override suspend fun count(): Long = localDataSource.count()
}
