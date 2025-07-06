package com.kelsos.mbrc.features.library.genres

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class Genre(val genre: String, val id: Long)

@JsonClass(generateAdapter = true)
data class GenreDto(
  @Json(name = "genre")
  val genre: String = "",
  @Json(name = "count")
  val count: Int = 0
)

@Entity(
  tableName = "genre",
  indices = []
)
data class GenreEntity(
  @ColumnInfo
  val genre: String? = null,
  @ColumnInfo
  val count: Int? = null,
  @ColumnInfo(name = "date_added")
  val dateAdded: Long? = null,
  @PrimaryKey(autoGenerate = true)
  val id: Long? = null
)
