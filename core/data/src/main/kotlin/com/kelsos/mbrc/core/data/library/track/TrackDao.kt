package com.kelsos.mbrc.core.data.library.track

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
    """
  )
  fun getAll(): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """
  )
  fun all(): List<TrackEntity>

  @Query(
    """
    select * from track
    where title LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%'
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """
  )
  fun search(term: String): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track where album = :album and album_artist = :artist
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """
  )
  fun getAlbumTracks(album: String, artist: String): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track where album = '' and (album_artist = :artist or :artist = '')
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """
  )
  fun getNonAlbumTracks(artist: String): PagingSource<Int, TrackEntity>

  @Query(
    """
    select distinct track.src from track
      inner join track_genre tg on tg.track_id = track.id
      inner join genre g on g.id = tg.genre_id
    where g.genre in (:genres)
    order by track.album_artist collate nocase asc, track.album collate nocase asc,
      track.disc asc, track.trackno asc
    """
  )
  fun getGenreTrackPaths(genres: List<String>): List<String>

  @Query(
    """
    select distinct track.src from track
      inner join track_artist ta on ta.track_id = track.id
      inner join artist a on a.id = ta.artist_id
    where a.artist in (:artists)
    order by track.album_artist collate nocase asc, track.album collate nocase asc,
      track.disc asc, track.trackno asc
    """
  )
  fun getArtistTrackPaths(artists: List<String>): List<String>

  @Query(
    """
    select src from track
    where album = :album and ((album_artist = :artist or artist = :artist) or :artist = '')
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """
  )
  fun getAlbumTrackPaths(album: String, artist: String): List<String>

  @Query(
    """
    select src from track
    order by album_artist collate nocase asc, album collate nocase asc, disc asc, trackno asc
    """
  )
  fun getAllTrackPaths(): List<String>

  @Query("select count(*) from track")
  fun count(): Long

  @Query("select id, artist, album_artist, genre from track")
  fun tagRows(): List<TrackTagRow>

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

  @Query("select * from track where src = :path")
  fun getByPath(path: String): TrackEntity?

  // Sorted getAll methods
  @Query("select * from track order by title collate nocase asc")
  fun getAllByTitleAsc(): PagingSource<Int, TrackEntity>

  @Query("select * from track order by title collate nocase desc")
  fun getAllByTitleDesc(): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track
    order by
      CASE
        WHEN LOWER(artist) LIKE 'the %' THEN SUBSTR(artist, 5)
        ELSE artist
      END COLLATE NOCASE ASC,
      album collate nocase asc, disc asc, trackno asc
    """
  )
  fun getAllByArtistAsc(): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track
    order by
      CASE
        WHEN LOWER(artist) LIKE 'the %' THEN SUBSTR(artist, 5)
        ELSE artist
      END COLLATE NOCASE DESC,
      album collate nocase asc, disc asc, trackno asc
    """
  )
  fun getAllByArtistDesc(): PagingSource<Int, TrackEntity>

  @Query("select * from track order by album collate nocase asc, disc asc, trackno asc")
  fun getAllByAlbumAsc(): PagingSource<Int, TrackEntity>

  @Query("select * from track order by album collate nocase desc, disc asc, trackno asc")
  fun getAllByAlbumDesc(): PagingSource<Int, TrackEntity>

  // Sorted search methods
  @Query(
    """
    select * from track
    where title LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%'
    order by title collate nocase asc
    """
  )
  fun searchByTitleAsc(term: String): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track
    where title LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%'
    order by title collate nocase desc
    """
  )
  fun searchByTitleDesc(term: String): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track
    where title LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%'
    order by
      CASE
        WHEN LOWER(artist) LIKE 'the %' THEN SUBSTR(artist, 5)
        ELSE artist
      END COLLATE NOCASE ASC,
      album collate nocase asc, disc asc, trackno asc
    """
  )
  fun searchByArtistAsc(term: String): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track
    where title LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%'
    order by
      CASE
        WHEN LOWER(artist) LIKE 'the %' THEN SUBSTR(artist, 5)
        ELSE artist
      END COLLATE NOCASE DESC,
      album collate nocase asc, disc asc, trackno asc
    """
  )
  fun searchByArtistDesc(term: String): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track
    where title LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%'
    order by album collate nocase asc, disc asc, trackno asc
    """
  )
  fun searchByAlbumAsc(term: String): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track
    where title LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%'
    order by album collate nocase desc, disc asc, trackno asc
    """
  )
  fun searchByAlbumDesc(term: String): PagingSource<Int, TrackEntity>

  // Album artist sorting
  @Query(
    """
    select * from track
    order by
      CASE
        WHEN LOWER(album_artist) LIKE 'the %' THEN SUBSTR(album_artist, 5)
        ELSE album_artist
      END COLLATE NOCASE ASC,
      album collate nocase asc, disc asc, trackno asc
    """
  )
  fun getAllByAlbumArtistAsc(): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track
    order by
      CASE
        WHEN LOWER(album_artist) LIKE 'the %' THEN SUBSTR(album_artist, 5)
        ELSE album_artist
      END COLLATE NOCASE DESC,
      album collate nocase asc, disc asc, trackno asc
    """
  )
  fun getAllByAlbumArtistDesc(): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track
    where title LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%'
    order by
      CASE
        WHEN LOWER(album_artist) LIKE 'the %' THEN SUBSTR(album_artist, 5)
        ELSE album_artist
      END COLLATE NOCASE ASC,
      album collate nocase asc, disc asc, trackno asc
    """
  )
  fun searchByAlbumArtistAsc(term: String): PagingSource<Int, TrackEntity>

  @Query(
    """
    select * from track
    where title LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%'
    order by
      CASE
        WHEN LOWER(album_artist) LIKE 'the %' THEN SUBSTR(album_artist, 5)
        ELSE album_artist
      END COLLATE NOCASE DESC,
      album collate nocase asc, disc asc, trackno asc
    """
  )
  fun searchByAlbumArtistDesc(term: String): PagingSource<Int, TrackEntity>
}
