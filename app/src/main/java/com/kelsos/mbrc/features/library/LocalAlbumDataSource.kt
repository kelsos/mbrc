package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.data.LocalDataSource
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.data.Database
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
    private val dispatchers: AppCoroutineDispatchers,
  ) : LocalDataSource<Album> {
    override suspend fun deleteAll() =
      withContext(dispatchers.database) {
        delete(Album::class).execute()
      }

    override suspend fun saveAll(list: List<Album>) =
      withContext(dispatchers.database) {
        val adapter = modelAdapter<Album>()
        val updated = list.filter { it.id > 0 }
        val inserted = list.filter { it.id <= 0 }

        val transaction =
          FastStoreModelTransaction
            .insertBuilder(adapter)
            .addAll(inserted)
            .build()

        val updateTransaction =
          FastStoreModelTransaction
            .updateBuilder(adapter)
            .addAll(updated)
            .build()

        database<Database>().executeTransaction(transaction)
        database<Database>().executeTransaction(updateTransaction)
      }

    override suspend fun loadAllCursor(): FlowCursorList<Album> =
      withContext(dispatchers.database) {
        val query =
          (select from Album::class)
            .orderBy(Album_Table.artist, true)
            .orderBy(Album_Table.album, true)
        return@withContext FlowCursorList.Builder(Album::class.java).modelQueriable(query).build()
      }

    suspend fun getAlbumsByArtist(artist: String): FlowCursorList<Album> =
      withContext(dispatchers.database) {
        val selectAlbum =
          SQLite.select(Album_Table.album.withTable(), Album_Table.artist.withTable()).distinct()
        val artistOrAlbumArtist =
          clause(Track_Table.artist.withTable().`is`(artist))
            .or(Track_Table.album_artist.withTable().`is`(artist))
        val columns =
          clause(Track_Table.album.withTable().eq(Album_Table.album.withTable()))
            .and(Track_Table.album_artist.withTable().eq(Album_Table.artist.withTable()))
        val query =
          (
            selectAlbum from Album::class
              innerJoin Track::class
              on columns
              where artistOrAlbumArtist
          ).orderBy(Album_Table.artist.withTable(), true)
            .orderBy(Album_Table.album.withTable(), true)
        return@withContext FlowCursorList.Builder(Album::class.java).modelQueriable(query).build()
      }

    override suspend fun search(term: String): FlowCursorList<Album> =
      withContext(dispatchers.database) {
        val query = (select from Album::class where Album_Table.album.like("%${term.escapeLike()}%"))
        return@withContext FlowCursorList.Builder(Album::class.java).modelQueriable(query).build()
      }

    override suspend fun isEmpty(): Boolean =
      withContext(dispatchers.database) {
        return@withContext SQLite.selectCountOf().from(Album::class.java).longValue() == 0L
      }

    override suspend fun count(): Long =
      withContext(dispatchers.database) {
        return@withContext SQLite.selectCountOf().from(Album::class.java).longValue()
      }

    suspend fun updateCovers(updated: List<CoverInfo>) {
      withContext(dispatchers.database) {
        val albums =
          (select from Album::class)
            .orderBy(Album_Table.artist, true)
            .orderBy(Album_Table.album, true)
            .queryList()

        for ((artist, album, hash) in updated) {
          val cachedAlbum = albums.find { it.album == album && it.artist == artist }
          cachedAlbum?.cover = hash
        }
        val adapter = modelAdapter<Album>()

        val transaction =
          FastStoreModelTransaction
            .updateBuilder(adapter)
            .addAll(albums)
            .build()

        database<Database>().executeTransaction(transaction)
      }
    }

    override suspend fun removePreviousEntries(epoch: Long) {
      withContext(dispatchers.database) {
        SQLite
          .delete()
          .from(Album::class.java)
          .where(clause(Album_Table.date_added.lessThan(epoch)).or(Album_Table.date_added.isNull))
          .execute()
      }
    }
  }
