package com.kelsos.mbrc.features.library.tracks

import com.kelsos.mbrc.R
import com.kelsos.mbrc.features.queue.Queue

fun determineTrackQueueAction(itemId: Int): Queue =
  when (itemId) {
    R.id.popup_track_queue_next -> Queue.Next
    R.id.popup_track_queue_last -> Queue.Last
    R.id.popup_track_play -> Queue.Now
    R.id.popup_track_play_queue_all -> Queue.AddAll
    R.id.popup_track_play_artist -> Queue.PlayArtist
    R.id.popup_track_play_album -> Queue.PlayAlbum
    else -> Queue.Now
  }
