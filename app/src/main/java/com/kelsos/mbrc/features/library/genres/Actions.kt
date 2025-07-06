package com.kelsos.mbrc.features.library.genres

import android.content.Context
import android.content.Intent
import androidx.annotation.IdRes
import com.kelsos.mbrc.R
import com.kelsos.mbrc.features.library.artists.GenreArtistsActivity
import com.kelsos.mbrc.features.queue.Queue

fun determineGenreQueueAction(@IdRes itemId: Int): Queue {
  if (R.id.popup_genre_artists == itemId) {
    return Queue.Default
  }

  return when (itemId) {
    R.id.popup_genre_queue_next -> Queue.Next
    R.id.popup_genre_queue_last -> Queue.Last
    R.id.popup_genre_play -> Queue.Now
    else -> Queue.Now
  }
}

fun Context.openGenreDetails(genre: Genre) {
  val intent = Intent(this, GenreArtistsActivity::class.java)
  intent.putExtra(GenreArtistsActivity.GENRE_ID, genre.id)
  intent.putExtra(GenreArtistsActivity.GENRE_NAME, genre.genre)
  this.startActivity(intent)
}
