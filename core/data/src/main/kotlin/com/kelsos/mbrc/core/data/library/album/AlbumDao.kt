package com.kelsos.mbrc.core.data.library.album

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AlbumDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(list: List<AlbumEntity>)

  @Query(
    """
    SELECT
      CASE WHEN album = '' THEN '' ELSE artist END AS artist,
      album,
      MIN(date_added) AS date_added,
      MIN(id) AS id,
      NULL AS cover
    FROM album
    GROUP BY
      CASE WHEN album = '' THEN '' ELSE artist END,
      album
    ORDER BY album COLLATE NOCASE ASC
    """
  )
  fun getAll(): PagingSource<Int, AlbumEntity>

  // Sort by album name ASC
  @Query(
    """
    SELECT
      CASE WHEN album = '' THEN '' ELSE artist END AS artist,
      album,
      MIN(date_added) AS date_added,
      MIN(id) AS id,
      NULL AS cover
    FROM album
    GROUP BY
      CASE WHEN album = '' THEN '' ELSE artist END,
      album
    ORDER BY album COLLATE NOCASE ASC
    """
  )
  fun getAllByNameAsc(): PagingSource<Int, AlbumEntity>

  // Sort by album name DESC
  @Query(
    """
    SELECT
      CASE WHEN album = '' THEN '' ELSE artist END AS artist,
      album,
      MIN(date_added) AS date_added,
      MIN(id) AS id,
      NULL AS cover
    FROM album
    GROUP BY
      CASE WHEN album = '' THEN '' ELSE artist END,
      album
    ORDER BY album COLLATE NOCASE DESC
    """
  )
  fun getAllByNameDesc(): PagingSource<Int, AlbumEntity>

  // Sort by artist ASC (ignoring "The" prefix)
  @Query(
    """
    SELECT
      CASE WHEN album = '' THEN '' ELSE artist END AS artist,
      album,
      MIN(date_added) AS date_added,
      MIN(id) AS id,
      NULL AS cover
    FROM album
    GROUP BY
      CASE WHEN album = '' THEN '' ELSE artist END,
      album
    ORDER BY
      CASE
        WHEN LOWER(CASE WHEN album = '' THEN '' ELSE artist END) LIKE 'the %'
        THEN SUBSTR(CASE WHEN album = '' THEN '' ELSE artist END, 5)
        ELSE CASE WHEN album = '' THEN '' ELSE artist END
      END COLLATE NOCASE ASC,
      album COLLATE NOCASE ASC
    """
  )
  fun getAllByArtistAsc(): PagingSource<Int, AlbumEntity>

  // Sort by artist DESC (ignoring "The" prefix)
  @Query(
    """
    SELECT
      CASE WHEN album = '' THEN '' ELSE artist END AS artist,
      album,
      MIN(date_added) AS date_added,
      MIN(id) AS id,
      NULL AS cover
    FROM album
    GROUP BY
      CASE WHEN album = '' THEN '' ELSE artist END,
      album
    ORDER BY
      CASE
        WHEN LOWER(CASE WHEN album = '' THEN '' ELSE artist END) LIKE 'the %'
        THEN SUBSTR(CASE WHEN album = '' THEN '' ELSE artist END, 5)
        ELSE CASE WHEN album = '' THEN '' ELSE artist END
      END COLLATE NOCASE DESC,
      album COLLATE NOCASE ASC
    """
  )
  fun getAllByArtistDesc(): PagingSource<Int, AlbumEntity>

  // Search by album name ASC
  @Query(
    """
    SELECT
      CASE WHEN album = '' THEN '' ELSE artist END AS artist,
      album,
      MIN(date_added) AS date_added,
      MIN(id) AS id,
      NULL AS cover
    FROM album
    WHERE album LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%'
    GROUP BY
      CASE WHEN album = '' THEN '' ELSE artist END,
      album
    ORDER BY album COLLATE NOCASE ASC
    """
  )
  fun searchByNameAsc(term: String): PagingSource<Int, AlbumEntity>

  // Search by album name DESC
  @Query(
    """
    SELECT
      CASE WHEN album = '' THEN '' ELSE artist END AS artist,
      album,
      MIN(date_added) AS date_added,
      MIN(id) AS id,
      NULL AS cover
    FROM album
    WHERE album LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%'
    GROUP BY
      CASE WHEN album = '' THEN '' ELSE artist END,
      album
    ORDER BY album COLLATE NOCASE DESC
    """
  )
  fun searchByNameDesc(term: String): PagingSource<Int, AlbumEntity>

  // Search by artist ASC (ignoring "The" prefix)
  @Query(
    """
    SELECT
      CASE WHEN album = '' THEN '' ELSE artist END AS artist,
      album,
      MIN(date_added) AS date_added,
      MIN(id) AS id,
      NULL AS cover
    FROM album
    WHERE album LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%'
    GROUP BY
      CASE WHEN album = '' THEN '' ELSE artist END,
      album
    ORDER BY
      CASE
        WHEN LOWER(CASE WHEN album = '' THEN '' ELSE artist END) LIKE 'the %'
        THEN SUBSTR(CASE WHEN album = '' THEN '' ELSE artist END, 5)
        ELSE CASE WHEN album = '' THEN '' ELSE artist END
      END COLLATE NOCASE ASC,
      album COLLATE NOCASE ASC
    """
  )
  fun searchByArtistAsc(term: String): PagingSource<Int, AlbumEntity>

  // Search by artist DESC (ignoring "The" prefix)
  @Query(
    """
    SELECT
      CASE WHEN album = '' THEN '' ELSE artist END AS artist,
      album,
      MIN(date_added) AS date_added,
      MIN(id) AS id,
      NULL AS cover
    FROM album
    WHERE album LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%'
    GROUP BY
      CASE WHEN album = '' THEN '' ELSE artist END,
      album
    ORDER BY
      CASE
        WHEN LOWER(CASE WHEN album = '' THEN '' ELSE artist END) LIKE 'the %'
        THEN SUBSTR(CASE WHEN album = '' THEN '' ELSE artist END, 5)
        ELSE CASE WHEN album = '' THEN '' ELSE artist END
      END COLLATE NOCASE DESC,
      album COLLATE NOCASE ASC
    """
  )
  fun searchByArtistDesc(term: String): PagingSource<Int, AlbumEntity>

  /**
   * Derives album rows from the track table, keyed by (album_artist, album). Uses
   * INSERT OR IGNORE so existing rows keep their cover hash and id; orphans are
   * removed separately by [removeOrphans].
   */
  @Query(
    """
    INSERT OR IGNORE INTO album (artist, album, date_added)
    SELECT DISTINCT album_artist, album, :added FROM track
    """
  )
  fun deriveFromTracks(added: Long)

  /**
   * Drops albums with no backing track.
   *
   * Written as `not exists` rather than the row-value form
   * `where (artist, album) not in (select album_artist, album from track)`, which reads better but
   * needs SQLite 3.15. That arrives with API 26, and `minSdk` is 23, so on Android 6 and 7 the
   * statement fails to prepare and takes every sync down with it. Host tests link a modern SQLite
   * and cannot catch that.
   */
  @Query(
    """
    delete from album
    where not exists (
      select 1 from track
      where track.album_artist = album.artist and track.album = album.album
    )
    """
  )
  fun removeOrphans()

  @Query("select count(*) from album")
  fun count(): Long

  @Query("select * from album")
  fun all(): List<AlbumEntity>

  @Query("delete from album where date_added < :added")
  fun removePreviousEntries(added: Long)

  // Get albums by artist sorted by album name ASC
  @Query(
    """
        SELECT album.artist AS artist, album.album AS album,
        album.date_added AS date_added, album.id AS id, album.cover AS cover
        FROM album
        INNER JOIN track ON album.album = track.album AND track.album_artist = album.artist
        INNER JOIN track_artist ta ON ta.track_id = track.id
        INNER JOIN artist a ON a.id = ta.artist_id
        WHERE a.artist = :artist
        GROUP BY album.id
        ORDER BY album.album COLLATE NOCASE ASC
    """
  )
  fun getAlbumsByArtistByNameAsc(artist: String): PagingSource<Int, AlbumEntity>

  // Get albums by artist sorted by album name DESC
  @Query(
    """
        SELECT album.artist AS artist, album.album AS album,
        album.date_added AS date_added, album.id AS id, album.cover AS cover
        FROM album
        INNER JOIN track ON album.album = track.album AND track.album_artist = album.artist
        INNER JOIN track_artist ta ON ta.track_id = track.id
        INNER JOIN artist a ON a.id = ta.artist_id
        WHERE a.artist = :artist
        GROUP BY album.id
        ORDER BY album.album COLLATE NOCASE DESC
    """
  )
  fun getAlbumsByArtistByNameDesc(artist: String): PagingSource<Int, AlbumEntity>

  @Query("select album, artist, cover as hash from album")
  fun getCovers(): List<AlbumCover>

  @Query("select count(*) from album where cover is not null")
  fun coverCount(): Long

  @Query("update album set cover = :cover where artist = :artist and album = :album")
  fun updateCover(artist: String, album: String, cover: String)

  @Transaction
  fun updateCovers(updated: List<AlbumCover>) {
    for ((artist, album, hash) in updated) {
      if (hash.isNullOrEmpty()) {
        continue
      }
      updateCover(
        artist = artist.orEmpty(),
        album = album.orEmpty(),
        cover = hash
      )
    }
  }

  @Query("select * from album where id = :id")
  fun getById(id: Long): AlbumEntity?

  @Query(
    """
        SELECT DISTINCT album.artist AS artist, album.album AS album,
        album.date_added AS date_added, album.id AS id, album.cover AS cover
        FROM album
        INNER JOIN track ON album.album = track.album AND track.album_artist = album.artist
        INNER JOIN track_genre ON track_genre.track_id = track.id
        WHERE track_genre.genre_id = :genreId
        GROUP BY album.id
        ORDER BY album.album COLLATE NOCASE ASC
    """
  )
  fun getAlbumsByGenre(genreId: Long): PagingSource<Int, AlbumEntity>

  // Get albums by genre sorted by album name ASC
  @Query(
    """
        SELECT DISTINCT album.artist AS artist, album.album AS album,
        album.date_added AS date_added, album.id AS id, album.cover AS cover
        FROM album
        INNER JOIN track ON album.album = track.album AND track.album_artist = album.artist
        INNER JOIN track_genre ON track_genre.track_id = track.id
        WHERE track_genre.genre_id = :genreId
        GROUP BY album.id
        ORDER BY album.album COLLATE NOCASE ASC
    """
  )
  fun getAlbumsByGenreByNameAsc(genreId: Long): PagingSource<Int, AlbumEntity>

  // Get albums by genre sorted by album name DESC
  @Query(
    """
        SELECT DISTINCT album.artist AS artist, album.album AS album,
        album.date_added AS date_added, album.id AS id, album.cover AS cover
        FROM album
        INNER JOIN track ON album.album = track.album AND track.album_artist = album.artist
        INNER JOIN track_genre ON track_genre.track_id = track.id
        WHERE track_genre.genre_id = :genreId
        GROUP BY album.id
        ORDER BY album.album COLLATE NOCASE DESC
    """
  )
  fun getAlbumsByGenreByNameDesc(genreId: Long): PagingSource<Int, AlbumEntity>

  // Get albums by genre sorted by artist ASC (ignoring "The" prefix)
  @Query(
    """
        SELECT DISTINCT album.artist AS artist, album.album AS album,
        album.date_added AS date_added, album.id AS id, album.cover AS cover
        FROM album
        INNER JOIN track ON album.album = track.album AND track.album_artist = album.artist
        INNER JOIN track_genre ON track_genre.track_id = track.id
        WHERE track_genre.genre_id = :genreId
        GROUP BY album.id
        ORDER BY
          CASE
            WHEN LOWER(album.artist) LIKE 'the %' THEN SUBSTR(album.artist, 5)
            ELSE album.artist
          END COLLATE NOCASE ASC,
          album.album COLLATE NOCASE ASC
    """
  )
  fun getAlbumsByGenreByArtistAsc(genreId: Long): PagingSource<Int, AlbumEntity>

  // Get albums by genre sorted by artist DESC (ignoring "The" prefix)
  @Query(
    """
        SELECT DISTINCT album.artist AS artist, album.album AS album,
        album.date_added AS date_added, album.id AS id, album.cover AS cover
        FROM album
        INNER JOIN track ON album.album = track.album AND track.album_artist = album.artist
        INNER JOIN track_genre ON track_genre.track_id = track.id
        WHERE track_genre.genre_id = :genreId
        GROUP BY album.id
        ORDER BY
          CASE
            WHEN LOWER(album.artist) LIKE 'the %' THEN SUBSTR(album.artist, 5)
            ELSE album.artist
          END COLLATE NOCASE DESC,
          album.album COLLATE NOCASE ASC
    """
  )
  fun getAlbumsByGenreByArtistDesc(genreId: Long): PagingSource<Int, AlbumEntity>
}
