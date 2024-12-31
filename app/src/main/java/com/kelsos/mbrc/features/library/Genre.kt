package com.kelsos.mbrc.features.library

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
@JsonPropertyOrder("genre", "count")
@Table(name = "genre", database = Database::class)
data class Genre(
  @JsonProperty("genre")
  @Column
  var genre: String? = null,
  @JsonProperty("count")
  @Column
  var count: Int = 0,
  @JsonIgnore
  @Column(name = "date_added")
  var dateAdded: Long = 0,
  @JsonIgnore
  @Column
  @PrimaryKey(autoincrement = true)
  var id: Long = 0,
) : Data
