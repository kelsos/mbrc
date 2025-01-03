package com.kelsos.mbrc.features.library

import android.app.Activity
import androidx.core.os.BundleCompat
import com.kelsos.mbrc.features.library.albums.AlbumInfo
import com.kelsos.mbrc.features.library.tracks.AlbumTracksActivity
import com.kelsos.mbrc.features.library.tracks.AlbumTracksActivity.Companion.ALBUM

fun Activity.extraId(
  key: String,
  default: Long = -1,
) = lazy {
  val value = intent?.extras?.getLong(key)
  value ?: default
}

fun Activity.extraString(
  key: String,
  default: String = "",
) = lazy {
  val value = intent?.extras?.getString(key)
  value ?: default
}

fun AlbumTracksActivity.albumInfo() =
  lazy {
    val extras = intent?.extras
    extras?.let { BundleCompat.getParcelable(it, ALBUM, AlbumInfo::class.java) }
  }
