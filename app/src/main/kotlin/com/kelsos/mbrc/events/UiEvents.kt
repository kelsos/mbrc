package com.kelsos.mbrc.events

import android.support.annotation.StringRes
import com.kelsos.mbrc.annotations.Connection
import com.kelsos.mbrc.annotations.PlayerState
import com.kelsos.mbrc.annotations.Repeat
import com.kelsos.mbrc.domain.TrackInfo
import com.kelsos.mbrc.enums.DiscoveryStop
import com.kelsos.mbrc.enums.LfmStatus

class ConnectionSettingsChanged(val defaultId: Long)

class ConnectionStatusChangeEvent(@Connection.Status val status: Int)

class CoverChangedEvent(val path: String = "")

class DiscoveryStopped(val reason: DiscoveryStop)

class LfmRatingChanged(val status: LfmStatus)

class LibraryRefreshCompleteEvent

class LyricsUpdatedEvent(val lyrics: String)

class NotifyUser {
  val message: String
  val resId: Int
  var isFromResource: Boolean = false
    private set

  constructor(message: String) {
    this.message = message
    this.isFromResource = false
    this.resId = -1
  }

  constructor(@StringRes resId: Int) {
    this.resId = resId
    this.isFromResource = true
    this.message = ""
  }
}

class OnMainFragmentOptionsInflated

class PlayStateChange(@PlayerState.State val state: String)

class RatingChanged(val rating: Float)

class RemoteClientMetaData(val trackInfo: TrackInfo, val coverPath: String = "")

class RepeatChange(@Repeat.Mode val mode: String)

class RequestConnectionStateEvent

class ScrobbleChange(val isActive: Boolean)

class ShuffleChange(@ShuffleState val shuffleState: String) {

  @android.support.annotation.StringDef(OFF, AUTODJ, SHUFFLE)
  @Retention(AnnotationRetention.SOURCE)
  annotation class ShuffleState

  companion object {
    const val OFF = "off"
    const val AUTODJ = "autodj"
    const val SHUFFLE = "shuffle"
  }
}

class TrackInfoChangeEvent(val trackInfo: TrackInfo)

class TrackMoved(val from:Int, val to:Int , val success: Boolean)

class TrackRemoval(val index: Int, val success: Boolean)

class UpdatePosition(val current: Int, val total: Int)

class VolumeChange {
  var volume: Int = 0
    private set
  var isMute: Boolean = false
    private set

  constructor(vol: Int) {
    this.volume = vol
    this.isMute = false
  }

  constructor() {
    this.volume = 0
    this.isMute = true
  }
}
