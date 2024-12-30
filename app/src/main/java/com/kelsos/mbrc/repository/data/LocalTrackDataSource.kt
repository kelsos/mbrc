package com.kelsos.mbrc.repository.data


import com.kelsos.mbrc.data.db.RemoteDatabase
import com.kelsos.mbrc.data.library.Track
import com.kelsos.mbrc.data.library.Track_Table
import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.extensions.escapeLike
import com.raizlabs.android.dbflow.kotlinextensions.and
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

class LocalTrackDataSource
@Inject constructor(private val dispatchers: AppDispatchers) : LocalDataSource<Track> {
  override suspend fun deleteAll() = withContext(dispatchers.db) {
    delete(Track::class).execute()
  }

  override suspend fun saveAll(list: List<Track>) = withContext(dispatchers.db) {
    val adapter = modelAdapter<Track>()

    val transaction = FastStoreModelTransaction.insertBuilder(adapter)
      .addAll(list)
      .build()

    database<RemoteDatabase>().executeTransaction(transaction)
  }

  override suspend fun loadAllCursor(): FlowCursorList<Track> = withContext(dispatchers.db) {
    val query = (select from Track::class)
      .orderBy(Track_Table.album_artist, true)
      .orderBy(Track_Table.album, true)
      .orderBy(Track_Table.disc, true)
      .orderBy(Track_Table.trackno, true)

    return@withContext FlowCursorList.Builder(Track::class.java).modelQueriable(query).build()
  }

  suspend fun getAlbumTracks(album: String, artist: String): FlowCursorList<Track> =
    withContext(dispatchers.db) {
      val query = (select from Track::class
          where Track_Table.album.`is`(album)
          and Track_Table.album_artist.`is`(artist))
        .orderBy(Track_Table.album_artist, true)
        .orderBy(Track_Table.album, true)
        .orderBy(Track_Table.disc, true)
        .orderBy(Track_Table.trackno, true)
      return@withContext FlowCursorList.Builder(Track::class.java).modelQueriable(query)
        .build()
    }

  suspend fun getNonAlbumTracks(artist: String): FlowCursorList<Track> =
    withContext(dispatchers.db) {
      val query = (select from Track::class
          where Track_Table.album.`is`("")
          and Track_Table.artist.`is`(artist))
        .orderBy(Track_Table.album_artist, true)
        .orderBy(Track_Table.album, true)
        .orderBy(Track_Table.disc, true)
        .orderBy(Track_Table.trackno, true)

      return@withContext FlowCursorList.Builder(Track::class.java).modelQueriable(query)
        .build()

    }

  override suspend fun search(term: String): FlowCursorList<Track> = withContext(dispatchers.db) {
    val query = (select from Track::class where Track_Table.title.like("%${term.escapeLike()}%"))
    return@withContext FlowCursorList.Builder(Track::class.java).modelQueriable(query)
      .build()
  }

  suspend fun getGenreTrackPaths(genre: String): List<String> =
    withContext(dispatchers.db) {
      return@withContext (select from Track::class
          where Track_Table.genre.`is`(genre))
        .orderBy(Track_Table.album_artist, true)
        .orderBy(Track_Table.album, true)
        .orderBy(Track_Table.disc, true)
        .orderBy(Track_Table.trackno, true)
        .queryList().filter { !it.src.isNullOrEmpty() }.map { it.src!! }
    }

  suspend fun getArtistTrackPaths(artist: String): List<String> =
    withContext(dispatchers.db) {
      return@withContext SQLite.select().from(Track::class).where(Track_Table.artist.`is`(artist))
        .or(Track_Table.album_artist.`is`(artist))
        .orderBy(Track_Table.album, true)
        .orderBy(Track_Table.disc, true)
        .orderBy(Track_Table.trackno, true)
        .queryList().filter { !it.src.isNullOrEmpty() }.map { it.src!! }
    }

  suspend fun getAlbumTrackPaths(album: String, artist: String): List<String> =
    withContext(dispatchers.db) {
      return@withContext (select from Track::class
          where Track_Table.album.`is`(album)
          and Track_Table.album_artist.`is`(artist))
        .orderBy(Track_Table.album_artist, true)
        .orderBy(Track_Table.album, true)
        .orderBy(Track_Table.disc, true)
        .orderBy(Track_Table.trackno, true)
        .queryList().filter { !it.src.isNullOrEmpty() }.map { it.src!! }
    }

  suspend fun getAllTrackPaths(): List<String> = withContext(dispatchers.db) {
    return@withContext (select from Track::class)
      .orderBy(Track_Table.album_artist, true)
      .orderBy(Track_Table.album, true)
      .orderBy(Track_Table.disc, true)
      .orderBy(Track_Table.trackno, true)
      .queryList().filter { !it.src.isNullOrEmpty() }.map { it.src!! }
  }

  override suspend fun isEmpty(): Boolean = withContext(dispatchers.db) {
    return@withContext SQLite.selectCountOf().from(Track::class.java).longValue() == 0L
  }

  override suspend fun count(): Long  = withContext(dispatchers.db) {
    return@withContext SQLite.selectCountOf().from(Track::class.java).longValue()
  }

  override suspend fun removePreviousEntries(epoch: Long) {
    withContext(dispatchers.db) {
      SQLite.delete()
        .from(Track::class.java)
        .where(clause(Track_Table.date_added.lessThan(epoch)).or(Track_Table.date_added.isNull))
        .execute()
    }
  }
}
