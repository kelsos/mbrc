package com.kelsos.mbrc.repository.data


import com.kelsos.mbrc.data.CoverInfo
import com.kelsos.mbrc.data.db.RemoteDatabase
import com.kelsos.mbrc.data.library.Album
import com.kelsos.mbrc.data.library.Album_Table
import com.kelsos.mbrc.data.library.Track
import com.kelsos.mbrc.data.library.Track_Table
import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.extensions.escapeLike
import com.raizlabs.android.dbflow.kotlinextensions.database
import com.raizlabs.android.dbflow.kotlinextensions.delete
import com.raizlabs.android.dbflow.kotlinextensions.from
import com.raizlabs.android.dbflow.kotlinextensions.innerJoin
import com.raizlabs.android.dbflow.kotlinextensions.modelAdapter
import com.raizlabs.android.dbflow.kotlinextensions.on
import com.raizlabs.android.dbflow.kotlinextensions.select
import com.raizlabs.android.dbflow.kotlinextensions.where
import com.raizlabs.android.dbflow.list.FlowCursorList
import com.raizlabs.android.dbflow.sql.language.OperatorGroup.clause
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.raizlabs.android.dbflow.structure.database.transaction.FastStoreModelTransaction
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalAlbumDataSource
@Inject
constructor(
  private val dispatchers: AppDispatchers
) : LocalDataSource<Album> {
  override suspend fun deleteAll() = withContext(dispatchers.db) {
    delete(Album::class).execute()
  }

  override suspend fun saveAll(list: List<Album>) = withContext(dispatchers.db) {
    val adapter = modelAdapter<Album>()
    val updated = list.filter { it.id > 0 }
    val inserted = list.filter { it.id <= 0 }

    val transaction = FastStoreModelTransaction.insertBuilder(adapter)
      .addAll(inserted)
      .build()

    val updateTransaction = FastStoreModelTransaction.updateBuilder(adapter)
      .addAll(updated)
      .build()

    database<RemoteDatabase>().executeTransaction(transaction)
    database<RemoteDatabase>().executeTransaction(updateTransaction)
  }

  override suspend fun loadAllCursor(): FlowCursorList<Album> = withContext(dispatchers.db) {
    val query = (select from Album::class)
      .orderBy(Album_Table.artist, true)
      .orderBy(Album_Table.album, true)
    return@withContext FlowCursorList.Builder(Album::class.java).modelQueriable(query).build()
  }

  suspend fun getAlbumsByArtist(artist: String): FlowCursorList<Album> =
    withContext(dispatchers.db) {
      val selectAlbum =
        SQLite.select(Album_Table.album.withTable(), Album_Table.artist.withTable()).distinct()
      val artistOrAlbumArtist = clause(Track_Table.artist.withTable().`is`(artist))
        .or(Track_Table.album_artist.withTable().`is`(artist))
      val columns = clause(Track_Table.album.withTable().eq(Album_Table.album.withTable()))
        .and(Track_Table.album_artist.withTable().eq(Album_Table.artist.withTable()))
      val query = (selectAlbum from Album::class
          innerJoin Track::class
          on columns
          where artistOrAlbumArtist)
        .orderBy(Album_Table.artist.withTable(), true)
        .orderBy(Album_Table.album.withTable(), true)
      return@withContext FlowCursorList.Builder(Album::class.java).modelQueriable(query).build()
    }

  override suspend fun search(term: String): FlowCursorList<Album> = withContext(dispatchers.db) {
    val query = (select from Album::class where Album_Table.album.like("%${term.escapeLike()}%"))
    return@withContext FlowCursorList.Builder(Album::class.java).modelQueriable(query).build()
  }

  override suspend fun isEmpty(): Boolean = withContext(dispatchers.db) {
    return@withContext SQLite.selectCountOf().from(Album::class.java).longValue() == 0L
  }

  override suspend fun count(): Long = withContext(dispatchers.db) {
    return@withContext SQLite.selectCountOf().from(Album::class.java).longValue()
  }

  suspend fun updateCovers(updated: List<CoverInfo>) {
    withContext(dispatchers.db) {
      val albums = (select from Album::class)
        .orderBy(Album_Table.artist, true)
        .orderBy(Album_Table.album, true)
        .queryList()

      for ((artist, album, hash) in updated) {
        val cachedAlbum = albums.find { it.album == album && it.artist == artist }
        cachedAlbum?.cover = hash
      }
      val adapter = modelAdapter<Album>()

      val transaction = FastStoreModelTransaction.updateBuilder(adapter)
        .addAll(albums)
        .build()

      database<RemoteDatabase>().executeTransaction(transaction)
    }
  }

  override suspend fun removePreviousEntries(epoch: Long) {
    withContext(dispatchers.db) {
      SQLite.delete()
        .from(Album::class.java)
        .where(clause(Album_Table.date_added.lessThan(epoch)).or(Album_Table.date_added.isNull))
        .execute()
    }
  }
}
