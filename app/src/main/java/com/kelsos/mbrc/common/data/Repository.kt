package com.kelsos.mbrc.common.data

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

typealias Progress = suspend (current: Int, total: Int) -> Unit

interface Repository<T : Any> {
  fun getAll(): Flow<PagingData<T>>

  suspend fun getRemote(progress: Progress? = null)

  fun search(term: String): Flow<PagingData<T>>

  suspend fun count(): Long

  suspend fun getById(id: Long): T?
}

suspend fun <T : Any> Repository<T>.cacheIsEmpty(): Boolean = count() == 0L
