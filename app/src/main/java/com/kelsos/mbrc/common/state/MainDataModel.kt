package com.kelsos.mbrc.common.state

import com.kelsos.mbrc.annotations.PlayerState
import com.kelsos.mbrc.annotations.PlayerState.State
import com.kelsos.mbrc.annotations.Repeat
import com.kelsos.mbrc.annotations.Repeat.Mode
import com.kelsos.mbrc.constants.Const
import com.kelsos.mbrc.events.ui.ShuffleChange
import com.kelsos.mbrc.events.ui.ShuffleChange.ShuffleState
import com.kelsos.mbrc.features.player.LfmStatus
import com.kelsos.mbrc.features.player.TrackInfo
import com.kelsos.mbrc.networking.protocol.Protocol

class MainDataModel {
  var pluginUpdateAvailable: Boolean = false
  var pluginUpdateRequired: Boolean = false
  var minimumRequired: String = ""
  var position: Long = 0
  var duration: Long = 0
  var trackInfo: TrackInfo = TrackInfo()
  var coverPath: String = ""
  var rating: Float = 0f
  var volume: Int = 0

  @ShuffleState
  var shuffle: String = ShuffleChange.OFF
  var isScrobblingEnabled: Boolean = false
  var isMute: Boolean = false
  var lfmStatus: LfmStatus = LfmStatus.NORMAL
    private set
  var apiOutOfDate: Boolean = false
    private set

  var pluginVersion: String = "1.0.0"
    set(value) {
      if (value.isEmpty()) {
        return
      }
      field = value.substring(0, value.lastIndexOf('.'))
    }

  var pluginProtocol: Int = 2
    set(value) {
      field = value
      if (value < Protocol.PROTOCOL_VERSION_NUMBER) {
        apiOutOfDate = true
      }
    }

  @State
  var playState: String = PlayerState.UNDEFINED
    set(value) {
      @State val newState: String =
        when {
          Const.PLAYING.equals(value, ignoreCase = true) -> PlayerState.PLAYING
          Const.STOPPED.equals(value, ignoreCase = true) -> PlayerState.STOPPED
          Const.PAUSED.equals(value, ignoreCase = true) -> PlayerState.PAUSED
          else -> PlayerState.UNDEFINED
        }
      field = newState
    }

  @Mode
  var repeat: String
    private set

  init {
    repeat = Repeat.NONE
    rating = 0f

    lfmStatus = LfmStatus.NORMAL
    pluginVersion = Const.EMPTY
  }

  fun setLfmRating(rating: String) {
    lfmStatus =
      when (rating) {
        "Love" -> LfmStatus.LOVED
        "Ban" -> LfmStatus.BANNED
        else -> LfmStatus.NORMAL
      }
  }

  fun setRepeatState(repeat: String) {
    this.repeat =
      when {
        Protocol.ALL.equals(repeat, ignoreCase = true) -> Repeat.ALL
        Protocol.ONE.equals(repeat, ignoreCase = true) -> Repeat.ONE
        else -> Repeat.NONE
      }
  }
}
