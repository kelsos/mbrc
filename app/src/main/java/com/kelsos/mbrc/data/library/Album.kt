package com.kelsos.mbrc.data.library

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.kelsos.mbrc.data.Data
import com.kelsos.mbrc.data.db.RemoteDatabase
import com.kelsos.mbrc.utilities.RemoteUtils.sha1
import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table

@Table(name = "album", database = RemoteDatabase::class)
data class Album(
  @JsonProperty("artist")
  @Column
  var artist: String? = null,
  @JsonProperty("album")
  @Column
  var album: String? = null,
  @JsonProperty("count")
  @Column
  var count: Int = 0,
  @JsonIgnore
  @Column
  var cover: String? = null,
  @JsonIgnore
  @Column(name = "date_added")
  var dateAdded: Long = 0,
  @JsonIgnore
  @Column
  @PrimaryKey(autoincrement = true)
  var id: Long = 0,
) : Data

fun Album.key(): String = sha1("${artist}_$album")
