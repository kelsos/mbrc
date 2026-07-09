package com.kelsos.mbrc.core.data.library.genre

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
data class Genre(val genre: String, val id: Long)

@Entity(
  tableName = "genre",
  indices = [Index("genre", name = "genre_genre_idx", unique = true)]
)
data class GenreEntity(
  @ColumnInfo(collate = ColumnInfo.NOCASE)
  val genre: String,
  @ColumnInfo(name = "date_added")
  val dateAdded: Long = 0,
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0
)
