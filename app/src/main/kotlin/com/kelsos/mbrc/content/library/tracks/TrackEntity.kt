package com.kelsos.mbrc.content.library.tracks

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import com.kelsos.mbrc.interfaces.data.Data

@Entity(tableName = "track", indices = [Index("src", name = "track_src_index", unique = true)])
data class TrackEntity(
    @ColumnInfo
    override var artist: String = "",
    @ColumnInfo
    override var title: String = "",
    @ColumnInfo
    override var src: String = "",
    @ColumnInfo
    override var trackno: Int = 0,
    @ColumnInfo
    override var disc: Int = 0,
    @ColumnInfo(name = "album_artist")
    override var albumArtist: String = "",
    @ColumnInfo
    override var album: String = "",
    @ColumnInfo
    override var genre: String = "",
    @ColumnInfo
    override var year: String = "",
    @ColumnInfo(name = "date_added")
    var dateAdded: Long = 0,
    @PrimaryKey(autoGenerate = true)
    override var id: Long = 0
) : Data, Track
