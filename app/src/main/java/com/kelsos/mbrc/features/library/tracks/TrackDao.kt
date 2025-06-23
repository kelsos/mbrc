package com.kelsos.mbrc.features.library.tracks

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TrackDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  fun insertAll(list: List<TrackEntity>)

  @Query(
    """
    select * from track
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """,
  )
  fun getAll(): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """,
  )
  fun all(): List<TrackEntity>

  @Query(
    """
    select * from track
    where title LIKE '%' || :term || '%'
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """,
  )
  fun search(term: String): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track where album = :album and album_artist = :artist
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """,
  )
  fun getAlbumTracks(
    album: String,
    artist: String,
  ): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track where album = '' and album_artist = :artist
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """,
  )
  fun getNonAlbumTracks(artist: String): PagingSource<Int, TrackEntity>

  @Query(
    """
    select src from track where genre = :genre
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """,
  )
  fun getGenreTrackPaths(genre: String): List<String>

  @Query(
    """
    select src from track where artist = :artist or album_artist = :artist
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """,
  )
  fun getArtistTrackPaths(artist: String): List<String>

  @Query(
    """
    select src from track
    where (album_artist = :artist or artist = :artist) and album = :album
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """,
  )
  fun getAlbumTrackPaths(
    album: String,
    artist: String,
  ): List<String>

  @Query(
    """
    select src from track
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """,
  )
  fun getAllTrackPaths(): List<String>

  @Query("select count(*) from track")
  fun count(): Long

  @Query("delete from track where src in (:paths)")
  fun deletePaths(paths: List<String>)

  @Query("delete from track where date_added != :added")
  fun removePreviousEntries(added: Long)

  @Query("select src, id from track where src in (:paths)")
  fun findMatchingIds(paths: List<String>): List<TrackPath>

  @Update
  fun update(data: List<TrackEntity>)

  @Query("select * from track where id = :id")
  fun getById(id: Long): TrackEntity?
}
