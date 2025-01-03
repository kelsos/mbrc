package com.kelsos.mbrc.features.library.albums

import android.content.Context
import android.content.Intent
import androidx.annotation.IdRes
import com.kelsos.mbrc.R
import com.kelsos.mbrc.features.library.tracks.AlbumTracksActivity
import com.kelsos.mbrc.features.queue.AlbumMapper
import com.kelsos.mbrc.features.queue.Queue

fun determineAlbumQueueAction(
  @IdRes itemId: Int,
): Queue {
  if (itemId == R.id.popup_album_tracks) {
    return Queue.Default
  }

  return when (itemId) {
    R.id.popup_album_queue_next -> Queue.Next
    R.id.popup_album_queue_last -> Queue.Last
    R.id.popup_album_play -> Queue.Now
    else -> Queue.Now
  }
}

fun Context.openAlbumDetails(album: Album) {
  val mapper = AlbumMapper()
  val intent = Intent(this, AlbumTracksActivity::class.java)
  intent.putExtra(AlbumTracksActivity.Companion.ALBUM, mapper.map(album))
  this.startActivity(intent)
}
