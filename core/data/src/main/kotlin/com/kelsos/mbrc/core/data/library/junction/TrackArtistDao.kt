package com.kelsos.mbrc.core.data.library.junction

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrackArtistDao {
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  fun insertAll(list: List<TrackArtistEntity>)

  @Query("select count(*) from track_artist")
  fun count(): Long

  @Query("delete from track_artist")
  fun deleteAll()
}
