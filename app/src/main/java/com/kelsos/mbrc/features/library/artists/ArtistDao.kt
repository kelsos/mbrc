package com.kelsos.mbrc.features.library.artists

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArtistDao {
  @Query("delete from artist")
  fun deleteAll()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertAll(list: List<ArtistEntity>)

  @Query(
    """
      select distinct artist.id, artist.artist, artist.date_added, artist.count
      from artist
        inner join track on artist.artist = track.artist
        inner join genre on genre.genre = track.genre
      where genre.id = :genreId group by artist.artist order by artist.artist asc
      """,
  )
  fun getArtistByGenre(genreId: Long): PagingSource<Int, ArtistEntity>

  @Query("select * from artist where artist like '%' || :term || '%' ")
  fun search(term: String): PagingSource<Int, ArtistEntity>

  @Query("select count(*) from artist")
  fun count(): Long

  @Query("delete from artist where date_added < :added")
  fun removePreviousEntries(added: Long)

  @Query("select * from artist order by artist collate nocase asc")
  fun getAll(): PagingSource<Int, ArtistEntity>

  @Query("select * from artist order by artist collate nocase asc")
  fun all(): List<ArtistEntity>

  @Query(
    """
      select distinct artist.id, artist.artist, artist.date_added, artist.count
      from artist inner join track on artist.artist = track.artist
      where track.album_artist = artist.artist group by artist.artist order by artist.artist asc
    """,
  )
  fun getAlbumArtists(): PagingSource<Int, ArtistEntity>

  @Query("select * from artist where id = :id")
  fun getById(id: Long): ArtistEntity?
}
