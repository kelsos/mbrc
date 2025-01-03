package com.kelsos.mbrc.features.playlists

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaylistDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertAll(list: List<PlaylistEntity>)

  @Query("select * from playlists")
  fun getAll(): PagingSource<Int, PlaylistEntity>

  @Query("select * from playlists")
  fun all(): List<PlaylistEntity>

  @Query("select * from playlists where name like '%'|| :term ||'%'")
  fun search(term: String): PagingSource<Int, PlaylistEntity>

  @Query("select count(*) from playlists")
  fun count(): Long

  @Query("delete from playlists where date_added != :added")
  fun removePreviousEntries(added: Long)

  @Query("select * from playlists where id = :id")
  fun getById(id: Long): PlaylistEntity?
}
