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

  // Sort by album name ASC
  @Query("select * from album order by album collate nocase asc")
  fun getAllByNameAsc(): PagingSource<Int, AlbumEntity>

  // Sort by album name DESC
  @Query("select * from album order by album collate nocase desc")
  fun getAllByNameDesc(): PagingSource<Int, AlbumEntity>

  // Sort by artist ASC (ignoring "The" prefix)
  @Query(
    """
    select * from album
    order by
      CASE
        WHEN LOWER(artist) LIKE 'the %' THEN SUBSTR(artist, 5)
        ELSE artist
      END COLLATE NOCASE ASC,
      album COLLATE NOCASE ASC
    """
  )
  fun getAllByArtistAsc(): PagingSource<Int, AlbumEntity>

  // Sort by artist DESC (ignoring "The" prefix)
  @Query(
    """
    select * from album
    order by
      CASE
        WHEN LOWER(artist) LIKE 'the %' THEN SUBSTR(artist, 5)
        ELSE artist
      END COLLATE NOCASE DESC,
      album COLLATE NOCASE ASC
    """
  )
  fun getAllByArtistDesc(): PagingSource<Int, AlbumEntity>

  // Sort by year ASC (unknown years at end)
  @Query(
    """
    SELECT album.artist AS artist, album.album AS album,
      album.date_added AS date_added, album.id AS id, album.cover AS cover
    FROM album
    LEFT JOIN track ON album.album = track.album AND track.album_artist = album.artist
    GROUP BY album.id
    ORDER BY
      CASE WHEN MIN(track.sortable_year) IS NULL OR MIN(track.sortable_year) = '' THEN 1 ELSE 0 END ASC,
      MIN(track.sortable_year) ASC,
      album.album COLLATE NOCASE ASC
    """
  )
  fun getAllByYearAsc(): PagingSource<Int, AlbumEntity>

  // Sort by year DESC (unknown years at end)
  @Query(
    """
    SELECT album.artist AS artist, album.album AS album,
      album.date_added AS date_added, album.id AS id, album.cover AS cover
    FROM album
    LEFT JOIN track ON album.album = track.album AND track.album_artist = album.artist
    GROUP BY album.id
    ORDER BY
      CASE WHEN MIN(track.sortable_year) IS NULL OR MIN(track.sortable_year) = '' THEN 1 ELSE 0 END ASC,
      MIN(track.sortable_year) DESC,
      album.album COLLATE NOCASE ASC
    """
  )
  fun getAllByYearDesc(): PagingSource<Int, AlbumEntity>

  // Search by album name ASC
  @Query(
    """
    select * from album
    where album like '%' || :term || '%' or artist like '%' || :term || '%'
    order by album collate nocase asc
    """
  )
  fun searchByNameAsc(term: String): PagingSource<Int, AlbumEntity>

  // Search by album name DESC
  @Query(
    """
    select * from album
    where album like '%' || :term || '%' or artist like '%' || :term || '%'
    order by album collate nocase desc
    """
  )
  fun searchByNameDesc(term: String): PagingSource<Int, AlbumEntity>

  // Search by artist ASC (ignoring "The" prefix)
  @Query(
    """
    select * from album
    where album like '%' || :term || '%' or artist like '%' || :term || '%'
    order by
      CASE
        WHEN LOWER(artist) LIKE 'the %' THEN SUBSTR(artist, 5)
        ELSE artist
      END COLLATE NOCASE ASC,
      album COLLATE NOCASE ASC
    """
  )
  fun searchByArtistAsc(term: String): PagingSource<Int, AlbumEntity>

  // Search by artist DESC (ignoring "The" prefix)
  @Query(
    """
    select * from album
    where album like '%' || :term || '%' or artist like '%' || :term || '%'
    order by
      CASE
        WHEN LOWER(artist) LIKE 'the %' THEN SUBSTR(artist, 5)
        ELSE artist
      END COLLATE NOCASE DESC,
      album COLLATE NOCASE ASC
    """
  )
  fun searchByArtistDesc(term: String): PagingSource<Int, AlbumEntity>

  // Search by year ASC (unknown years at end)
  @Query(
    """
    SELECT album.artist AS artist, album.album AS album,
      album.date_added AS date_added, album.id AS id, album.cover AS cover
    FROM album
    LEFT JOIN track ON album.album = track.album AND track.album_artist = album.artist
    WHERE album.album LIKE '%' || :term || '%' OR album.artist LIKE '%' || :term || '%'
    GROUP BY album.id
    ORDER BY
      CASE WHEN MIN(track.sortable_year) IS NULL OR MIN(track.sortable_year) = '' THEN 1 ELSE 0 END ASC,
      MIN(track.sortable_year) ASC,
      album.album COLLATE NOCASE ASC
    """
  )
  fun searchByYearAsc(term: String): PagingSource<Int, AlbumEntity>

  // Search by year DESC (unknown years at end)
  @Query(
    """
    SELECT album.artist AS artist, album.album AS album,
      album.date_added AS date_added, album.id AS id, album.cover AS cover
    FROM album
    LEFT JOIN track ON album.album = track.album AND track.album_artist = album.artist
    WHERE album.album LIKE '%' || :term || '%' OR album.artist LIKE '%' || :term || '%'
    GROUP BY album.id
    ORDER BY
      CASE WHEN MIN(track.sortable_year) IS NULL OR MIN(track.sortable_year) = '' THEN 1 ELSE 0 END ASC,
      MIN(track.sortable_year) DESC,
      album.album COLLATE NOCASE ASC
    """
  )
  fun searchByYearDesc(term: String): PagingSource<Int, AlbumEntity>

  @Query("select count(*) from album")
  fun count(): Long

  @Query("select * from album")
  fun all(): List<AlbumEntity>

  @Query("delete from album where date_added < :added")
  fun removePreviousEntries(added: Long)

  // Get albums by artist sorted by year ASC (unknown years at end)
  @Query(
    """
        SELECT album.artist AS artist, album.album AS album,
        album.date_added AS date_added, album.id AS id, album.cover AS cover
        FROM album
        INNER JOIN track ON album.album = track.album AND track.album_artist = album.artist
        WHERE track.artist = :artist OR track.album_artist = :artist
        GROUP BY album.id
        ORDER BY
          CASE WHEN MIN(track.sortable_year) = '' THEN 1 ELSE 0 END ASC,
          MIN(track.sortable_year) ASC,
          album.album COLLATE NOCASE ASC
    """
  )
  fun getAlbumsByArtistByYearAsc(artist: String): PagingSource<Int, AlbumEntity>

  // Get albums by artist sorted by year DESC (unknown years at end)
  @Query(
    """
        SELECT album.artist AS artist, album.album AS album,
        album.date_added AS date_added, album.id AS id, album.cover AS cover
        FROM album
        INNER JOIN track ON album.album = track.album AND track.album_artist = album.artist
        WHERE track.artist = :artist OR track.album_artist = :artist
        GROUP BY album.id
        ORDER BY
          CASE WHEN MIN(track.sortable_year) = '' THEN 1 ELSE 0 END ASC,
          MIN(track.sortable_year) DESC,
          album.album COLLATE NOCASE ASC
    """
  )
  fun getAlbumsByArtistByYearDesc(artist: String): PagingSource<Int, AlbumEntity>

  // Get albums by artist sorted by album name ASC
  @Query(
    """
        SELECT album.artist AS artist, album.album AS album,
        album.date_added AS date_added, album.id AS id, album.cover AS cover
        FROM album
        INNER JOIN track ON album.album = track.album AND track.album_artist = album.artist
        WHERE track.artist = :artist OR track.album_artist = :artist
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
        WHERE track.artist = :artist OR track.album_artist = :artist
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
}
