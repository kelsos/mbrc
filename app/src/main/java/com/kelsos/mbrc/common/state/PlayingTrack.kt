package com.kelsos.mbrc.common.state

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi

data class PlayingTrack(
  val artist: String = "",
  val title: String = "",
  val album: String = "",
  val year: String = "",
  val path: String = "",
  val coverUrl: String = "",
  val duration: Long = 0
) : Parcelable {
  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<PlayingTrack> =
      object : Parcelable.Creator<PlayingTrack> {
        override fun createFromParcel(source: Parcel): PlayingTrack = PlayingTrack(source)

        override fun newArray(size: Int): Array<PlayingTrack?> = arrayOfNulls(size)
      }
  }

  constructor(source: Parcel) : this(
    source.readString().orEmpty(),
    source.readString().orEmpty(),
    source.readString().orEmpty(),
    source.readString().orEmpty(),
    source.readString().orEmpty(),
    source.readString().orEmpty(),
    source.readLong()
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(artist)
    dest.writeString(title)
    dest.writeString(album)
    dest.writeString(year)
    dest.writeString(path)
    dest.writeString(coverUrl)
    dest.writeLong(duration)
  }
}

fun PlayingTrack?.orEmpty() = this ?: PlayingTrack()

@OptIn(UnstableApi::class)
fun PlayingTrack.toMediaItem(): MediaItem {
  val metadata =
    MediaMetadata
      .Builder()
      .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
      .setTitle(title)
      .setArtist(artist)
      .setAlbumTitle(album)
      .setArtworkUri(coverUrl.toUri())
      .setReleaseYear(year.toIntOrNull() ?: 0)
      .setDisplayTitle(title)
      .setSubtitle(artist)
      .setDescription(album)
      .setDurationMs(duration)
      .build()

  return MediaItem
    .Builder()
    .setMediaId(path)
    .setMediaMetadata(metadata)
    .build()
}
