package com.kelsos.mbrc.repository.data

import com.kelsos.mbrc.data.NowPlaying
import com.kelsos.mbrc.data.NowPlaying_Table
import com.kelsos.mbrc.data.db.RemoteDatabase
import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.extensions.escapeLike
import com.raizlabs.android.dbflow.kotlinextensions.database
import com.raizlabs.android.dbflow.kotlinextensions.delete
import com.raizlabs.android.dbflow.kotlinextensions.from
import com.raizlabs.android.dbflow.kotlinextensions.modelAdapter
import com.raizlabs.android.dbflow.kotlinextensions.or
import com.raizlabs.android.dbflow.kotlinextensions.orderBy
import com.raizlabs.android.dbflow.kotlinextensions.select
import com.raizlabs.android.dbflow.kotlinextensions.where
import com.raizlabs.android.dbflow.list.FlowCursorList
import com.raizlabs.android.dbflow.sql.language.OperatorGroup.clause
import com.raizlabs.android.dbflow.sql.language.OrderBy
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.raizlabs.android.dbflow.structure.database.transaction.FastStoreModelTransaction
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalNowPlayingDataSource
@Inject constructor(private val dispatchers: AppDispatchers) : LocalDataSource<NowPlaying> {
  override suspend fun deleteAll() = withContext(dispatchers.db) {
    delete(NowPlaying::class).execute()
  }

  override suspend fun saveAll(list: List<NowPlaying>) = withContext(dispatchers.db) {
    val adapter = modelAdapter<NowPlaying>()

    val transaction = FastStoreModelTransaction.insertBuilder(adapter)
      .addAll(list)
      .build()

    database<RemoteDatabase>().executeTransaction(transaction)
  }

  override suspend fun loadAllCursor(): FlowCursorList<NowPlaying> = withContext(dispatchers.db) {
    val positionAscending = OrderBy.fromProperty(NowPlaying_Table.position).ascending()
    val query = (select from NowPlaying::class orderBy positionAscending)
    return@withContext FlowCursorList.Builder(NowPlaying::class.java).modelQueriable(query).build()
  }

  override suspend fun search(term: String): FlowCursorList<NowPlaying> =
    withContext(dispatchers.db) {
      val searchTerm = "%${term.escapeLike()}%"
      val query =
        (select from NowPlaying::class where NowPlaying_Table.title.like(searchTerm) or NowPlaying_Table.artist.like(
          searchTerm
        ))
      return@withContext FlowCursorList.Builder(NowPlaying::class.java).modelQueriable(query)
        .build()
    }

  override suspend fun isEmpty(): Boolean = withContext(dispatchers.db) {
    return@withContext SQLite.selectCountOf().from(NowPlaying::class.java).longValue() == 0L
  }

  override suspend fun count(): Long = withContext(dispatchers.db) {
    return@withContext SQLite.selectCountOf().from(NowPlaying::class.java).longValue()
  }

  override suspend fun removePreviousEntries(epoch: Long) {
    withContext(dispatchers.db) {
      SQLite.delete()
        .from(NowPlaying::class.java)
        .where(clause(NowPlaying_Table.date_added.lessThan(epoch)).or(NowPlaying_Table.date_added.isNull))
        .execute()
    }
  }
}
