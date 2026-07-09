package com.kelsos.mbrc.core.data.library.junction

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrackGenreDao {
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  fun insertAll(list: List<TrackGenreEntity>)

  @Query("select count(*) from track_genre")
  fun count(): Long

  @Query("delete from track_genre")
  fun deleteAll()
}
