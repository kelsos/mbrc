package com.kelsos.mbrc.content.library.albums

import android.os.Parcel
import android.os.Parcelable

data class AlbumInfo(val album: String, val artist: String) : Parcelable {
  constructor(source: Parcel): this(source.readString(), source.readString())

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel?, flags: Int) {
    dest?.writeString(album)
    dest?.writeString(artist)
  }

  companion object {
    @JvmField val CREATOR: Parcelable.Creator<AlbumInfo> = object : Parcelable.Creator<AlbumInfo> {
      override fun createFromParcel(source: Parcel): AlbumInfo = AlbumInfo(source)
      override fun newArray(size: Int): Array<AlbumInfo?> = arrayOfNulls(size)
    }
  }
}
