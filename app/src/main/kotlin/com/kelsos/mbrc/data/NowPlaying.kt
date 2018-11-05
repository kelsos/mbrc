package com.kelsos.mbrc.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.kelsos.mbrc.data.db.RemoteDatabase
import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)

@JsonPropertyOrder("artist", "title", "path", "position")
@Table(database = RemoteDatabase::class, name = "now_playing")
data class NowPlaying(@JsonProperty("title")
                      @Column(name = "title")
                      var title: String? = null,
                      @JsonProperty("artist")
                      @Column(name = "artist")
                      var artist: String? = null,
                      @JsonProperty("path")
                      @Column(name = "path")
                      var path: String? = null,
                      @JsonProperty("position")
                      @Column(name = "position")
                      var position: Int = 0,
                      @JsonIgnore
                      @PrimaryKey(autoincrement = true)
                      var id: Long = 0) : Data
