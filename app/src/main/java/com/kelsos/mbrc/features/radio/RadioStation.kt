package com.kelsos.mbrc.features.radio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class RadioStation(val name: String, val url: String, val id: Long)

@JsonClass(generateAdapter = true)
data class RadioStationDto(
  @Json(name = "name")
  val name: String = "",
  @Json(name = "url")
  val url: String = ""
)

@Entity(
  tableName = "radio_station",
  indices = [Index("url", name = "radio_url_idx", unique = true)]
)
data class RadioStationEntity(
  @ColumnInfo(name = "name")
  val name: String,
  @ColumnInfo(name = "url")
  val url: String,
  @ColumnInfo(name = "date_added")
  val dateAdded: Long = 0,
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0
)
