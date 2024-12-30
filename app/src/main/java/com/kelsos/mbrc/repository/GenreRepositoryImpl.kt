package com.kelsos.mbrc.repository

import com.kelsos.mbrc.data.library.Genre
import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.repository.data.LocalGenreDataSource
import com.kelsos.mbrc.repository.data.RemoteGenreDataSource
import com.raizlabs.android.dbflow.list.FlowCursorList
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import org.threeten.bp.Instant
import javax.inject.Inject

class GenreRepositoryImpl
  @Inject
  constructor(
    private val remoteDataSource: RemoteGenreDataSource,
    private val localDataSource: LocalGenreDataSource,
    private val dispatchers: AppDispatchers,
  ) : GenreRepository {
    override suspend fun getAllCursor(): FlowCursorList<Genre> = localDataSource.loadAllCursor()

    override suspend fun getAndSaveRemote(): FlowCursorList<Genre> {
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
          }.collect { genres ->
            val data = genres.map { it.apply { dateAdded = epoch } }
            localDataSource.saveAll(data)
          }
      }
    }

    override suspend fun search(term: String): FlowCursorList<Genre> = localDataSource.search(term)

    override suspend fun cacheIsEmpty(): Boolean = localDataSource.isEmpty()

    override suspend fun count(): Long = localDataSource.count()
  }
