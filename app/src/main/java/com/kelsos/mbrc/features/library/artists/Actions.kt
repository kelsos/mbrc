package com.kelsos.mbrc.features.library.artists

import android.content.Context
import android.content.Intent
import androidx.annotation.IdRes
import com.kelsos.mbrc.R
import com.kelsos.mbrc.features.library.albums.ArtistAlbumsActivity
import com.kelsos.mbrc.features.queue.Queue

fun determineArtistQueueAction(
  @IdRes itemId: Int,
): Queue {
  if (itemId == R.id.popup_artist_album) {
    return Queue.Default
  }

  return when (itemId) {
    R.id.popup_artist_queue_next -> Queue.Next
    R.id.popup_artist_queue_last -> Queue.Last
    R.id.popup_artist_play -> Queue.Now
    else -> Queue.Now
  }
}

fun Context.openArtistDetails(artist: Artist) {
  val intent = Intent(this, ArtistAlbumsActivity::class.java)
  intent.putExtra(ArtistAlbumsActivity.ARTIST_NAME, artist.artist)
  intent.putExtra(ArtistAlbumsActivity.ARTIST_ID, artist.id)
  this.startActivity(intent)
}
