package com.kelsos.mbrc.events

import android.support.annotation.StringRes
import com.kelsos.mbrc.content.activestatus.PlayerState
import com.kelsos.mbrc.content.library.tracks.PlayingTrackModel
import com.kelsos.mbrc.networking.connections.Connection

class ConnectionSettingsChanged(val defaultId: Long)

class ConnectionStatusChangeEvent(@Connection.Status val status: Int)

class LibraryRefreshCompleteEvent

class NotifyUser(@StringRes val resId: Int) {
  val message: String
  var isFromResource: Boolean = false
    private set

  init {
    this.isFromResource = true
    this.message = ""
  }

}

class PlayStateChange(@PlayerState.State val state: String)

class RatingChanged(val rating: Float)

class RemoteClientMetaData(val track: PlayingTrackModel, val coverPath: String = "")

