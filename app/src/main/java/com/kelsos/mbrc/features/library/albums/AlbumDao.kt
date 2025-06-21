package com.kelsos.mbrc.features.library.albums

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

  @Query("select * from album order by album collate nocase asc")
  fun getAll(): PagingSource<Int, AlbumEntity>

  @Query("select * from album where album like '%' || :term || '%'")
  fun search(term: String): PagingSource<Int, AlbumEntity>

  @Query("select count(*) from album")
  fun count(): Long

  @Query("select * from album")
  fun all(): List<AlbumEntity>

  @Query("delete from album where date_added < :added")
  fun removePreviousEntries(added: Long)

  @Query(
    """
        select distinct album.artist as artist, album.album as album, album.count as count,
        album.date_added as date_added, album.id as id, album.cover as cover from album
        inner join track where album.album = track.album and track.album_artist = album.artist
        and (track.artist = :artist or track.album_artist = :artist) order by artist collate nocase asc, album collate nocase asc
    """,
  )
  fun getAlbumsByArtist(artist: String): PagingSource<Int, AlbumEntity>

  @Query("select album, artist, cover as hash from album")
  fun getCovers(): List<AlbumCover>

  @Query("select count(*) from album where cover is not null")
  fun coverCount(): Long

  @Query("update album set cover = :cover where artist = :artist and album = :album")
  fun updateCover(
    artist: String,
    album: String,
    cover: String,
  )

  @Transaction
  fun updateCovers(updated: List<AlbumCover>) {
    for ((artist, album, hash) in updated) {
      if (hash.isNullOrEmpty()) {
        continue
      }
      updateCover(
        artist = artist.orEmpty(),
        album = album.orEmpty(),
        cover = hash,
      )
    }
  }

  @Query("select * from album where id = :id")
  fun getById(id: Long): AlbumEntity?
}
