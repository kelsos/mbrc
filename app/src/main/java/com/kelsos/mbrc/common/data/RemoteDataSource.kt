package com.kelsos.mbrc.common.data

import kotlinx.coroutines.flow.Flow

interface RemoteDataSource<T> {
  /**
   * Retrieves all the available data from a remote data source
   */
  suspend fun fetch(): Flow<List<T>>

  companion object {
    const val LIMIT = 800
  }
}
