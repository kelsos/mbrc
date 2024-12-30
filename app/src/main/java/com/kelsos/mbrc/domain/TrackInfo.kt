package com.kelsos.mbrc.domain

import android.os.Parcel
import android.os.Parcelable

data class TrackInfo(
  var artist: String = "",
  var title: String = "",
  var album: String = "",
  var year: String = "",
  var path: String = "",
) : Parcelable {
  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<TrackInfo> =
      object : Parcelable.Creator<TrackInfo> {
        override fun createFromParcel(source: Parcel): TrackInfo = TrackInfo(source)

        override fun newArray(size: Int): Array<TrackInfo?> = arrayOfNulls(size)
      }
  }

  constructor(source: Parcel) : this(
    source.readString() ?: "",
    source.readString() ?: "",
    source.readString() ?: "",
    source.readString() ?: "",
    source.readString() ?: "",
  )

  override fun describeContents() = 0

  override fun writeToParcel(
    dest: Parcel,
    flags: Int,
  ) {
    dest.writeString(artist)
    dest.writeString(title)
    dest.writeString(album)
    dest.writeString(year)
    dest.writeString(path)
  }
}

fun TrackInfo.isEmpty(): Boolean = artist.isEmpty() && title.isEmpty() && album.isEmpty() && year.isEmpty() && path.isEmpty()
