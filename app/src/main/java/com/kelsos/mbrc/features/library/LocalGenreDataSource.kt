package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.data.LocalDataSource
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.data.Database
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

class LocalGenreDataSource(
  private val dispatchers: AppCoroutineDispatchers,
) : LocalDataSource<Genre> {
  override suspend fun deleteAll() =
    withContext(dispatchers.database) {
      delete(Genre::class).execute()
    }

  override suspend fun saveAll(list: List<Genre>) =
    withContext(dispatchers.database) {
      val adapter = modelAdapter<Genre>()

      val transaction =
        FastStoreModelTransaction
          .insertBuilder(adapter)
          .addAll(list)
          .build()

      database<Database>().executeTransaction(transaction)
    }

  override suspend fun loadAllCursor(): FlowCursorList<Genre> =
    withContext(dispatchers.database) {
      val query = (select from Genre::class).orderBy(Genre_Table.genre, true)
      return@withContext FlowCursorList.Builder(Genre::class.java).modelQueriable(query).build()
    }

  override suspend fun search(term: String): FlowCursorList<Genre> =
    withContext(dispatchers.database) {
      val query =
        (select from Genre::class where Genre_Table.genre.like("%${term.escapeLike()}%"))
          .orderBy(Genre_Table.genre, true)
      return@withContext FlowCursorList.Builder(Genre::class.java).modelQueriable(query).build()
    }

  override suspend fun isEmpty(): Boolean =
    withContext(dispatchers.database) {
      return@withContext SQLite.selectCountOf().from(Genre::class.java).longValue() == 0L
    }

  override suspend fun count(): Long =
    withContext(dispatchers.database) {
      return@withContext SQLite.selectCountOf().from(Genre::class.java).longValue()
    }

  override suspend fun removePreviousEntries(epoch: Long) {
    withContext(dispatchers.io) {
      SQLite
        .delete()
        .from(Genre::class.java)
        .where(clause(Genre_Table.date_added.lessThan(epoch)).or(Genre_Table.date_added.isNull))
        .execute()
    }
  }
}
