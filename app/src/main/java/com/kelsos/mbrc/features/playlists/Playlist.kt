package com.kelsos.mbrc.features.playlists

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.kelsos.mbrc.data.Data
import com.kelsos.mbrc.data.Database
import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("name", "url")
@Table(database = Database::class, name = "playlists")
data class Playlist(
  @Column(name = "name")
  @JsonProperty var name: String = "",
  @Column(name = "url")
  @JsonProperty var url: String = "",
  @JsonIgnore
  @Column(name = "date_added")
  var dateAdded: Long = 0,
  @Column(name = "id")
  @PrimaryKey(autoincrement = true)
  @JsonIgnore
  var id: Long = 0,
) : Data
