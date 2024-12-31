package com.kelsos.mbrc.features.library

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.kelsos.mbrc.common.utilities.RemoteUtils
import com.kelsos.mbrc.data.Data
import com.kelsos.mbrc.data.Database
import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("artist", "title", "src", "trackno", "disc")
@Table(name = "track", database = Database::class)
data class Track(
  @JsonProperty("artist")
  @Column
  var artist: String? = null,
  @JsonProperty("title")
  @Column
  var title: String? = null,
  @JsonProperty("src")
  @Column
  var src: String? = null,
  @JsonProperty("trackno")
  @Column
  var trackno: Int = 0,
  @JsonProperty("disc")
  @Column
  var disc: Int = 0,
  @JsonProperty("album_artist")
  @Column(name = "album_artist")
  var albumArtist: String? = null,
  @JsonProperty("album")
  @Column
  var album: String? = null,
  @JsonProperty("genre")
  @Column
  var genre: String? = null,
  @JsonIgnore
  @Column(name = "date_added")
  var dateAdded: Long = 0,
  @JsonIgnore
  @Column
  @PrimaryKey(autoincrement = true)
  var id: Long = 0,
) : Data

fun Track.key(): String = RemoteUtils.sha1("${albumArtist}_$album")
