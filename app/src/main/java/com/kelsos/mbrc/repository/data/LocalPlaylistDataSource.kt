package com.kelsos.mbrc.repository.data

import com.kelsos.mbrc.data.Playlist
import com.kelsos.mbrc.data.Playlist_Table
import com.kelsos.mbrc.data.db.RemoteDatabase
import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.extensions.escapeLike
import com.raizlabs.android.dbflow.kotlinextensions.database
import com.raizlabs.android.dbflow.kotlinextensions.delete
import com.raizlabs.android.dbflow.kotlinextensions.from
import com.raizlabs.android.dbflow.kotlinextensions.modelAdapter
import com.raizlabs.android.dbflow.kotlinextensions.select
import com.raizlabs.android.dbflow.kotlinextensions.where
import com.raizlabs.android.dbflow.list.FlowCursorList
import com.raizlabs.android.dbflow.sql.language.OperatorGroup.clause
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.raizlabs.android.dbflow.structure.database.transaction.FastStoreModelTransaction
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalPlaylistDataSource
@Inject constructor(private val dispatchers: AppDispatchers) : LocalDataSource<Playlist> {
  override suspend fun deleteAll() = withContext(dispatchers.db) {
    delete(Playlist::class).execute()
  }

  override suspend fun saveAll(list: List<Playlist>) = withContext(dispatchers.db) {
    val adapter = modelAdapter<Playlist>()

    val transaction = FastStoreModelTransaction.insertBuilder(adapter)
      .addAll(list)
      .build()

    database<RemoteDatabase>().executeTransaction(transaction)
  }

  override suspend fun loadAllCursor(): FlowCursorList<Playlist> = withContext(dispatchers.db) {
    val query = (select from Playlist::class)
    return@withContext FlowCursorList.Builder(Playlist::class.java).modelQueriable(query).build()
  }

  override suspend fun search(term: String): FlowCursorList<Playlist> =
    withContext(dispatchers.db) {
      val query = (select from Playlist::class where Playlist_Table.name.like("%${term.escapeLike()}%"))
      return@withContext FlowCursorList.Builder(Playlist::class.java).modelQueriable(query).build()
    }

  override suspend fun isEmpty(): Boolean = withContext(dispatchers.db) {
    return@withContext SQLite.selectCountOf().from(Playlist::class.java).longValue() == 0L
  }

  override suspend fun count(): Long = withContext(dispatchers.db) {
    return@withContext SQLite.selectCountOf().from(Playlist::class.java).longValue()
  }

  override suspend fun removePreviousEntries(epoch: Long) {
    withContext(dispatchers.db) {
      SQLite.delete()
        .from(Playlist::class.java)
        .where(
          clause(Playlist_Table.date_added.lessThan(epoch)).or(
            Playlist_Table.date_added.isNull
          )
        ).execute()
    }
  }
}
