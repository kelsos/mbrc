package com.kelsos.mbrc.features.radio

import androidx.room.ColumnInfo
import androidx.room.Entity
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
  indices = []
)
data class RadioStationEntity(
  @ColumnInfo(name = "name")
  val name: String? = null,
  @ColumnInfo(name = "url")
  val url: String? = null,
  @ColumnInfo(name = "date_added")
  val dateAdded: Long? = null,
  @PrimaryKey(autoGenerate = true)
  val id: Long? = null
)
