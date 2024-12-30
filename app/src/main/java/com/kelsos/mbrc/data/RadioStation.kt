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

@JsonPropertyOrder("name", "url")
@Table(name = "radio_station", database = RemoteDatabase::class)
data class RadioStation(
    @JsonProperty("name")
    @Column(name = "name")
    var name: String = "",
    @JsonProperty("url")
    @Column(name = "url")
    var url: String = "",
    @JsonIgnore
    @Column(name="date_added")
    var dateAdded: Long = 0,
    @JsonIgnore
    @PrimaryKey(autoincrement = true)
    var id: Long = 0
) : Data
