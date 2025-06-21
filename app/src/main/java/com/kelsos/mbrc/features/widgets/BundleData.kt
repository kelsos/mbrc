package com.kelsos.mbrc.features.widgets

import android.os.Bundle
import androidx.core.os.BundleCompat
import com.kelsos.mbrc.common.state.PlayerState
import com.kelsos.mbrc.common.state.PlayingTrack

class BundleData(
  private val bundle: Bundle,
) {
  fun isState() = bundle.getBoolean(WidgetUpdater.STATE, false)

  fun isInfo() = bundle.getBoolean(WidgetUpdater.INFO, false)

  fun state(): String = bundle.getString(WidgetUpdater.PLAYER_STATE, PlayerState.UNDEFINED)

  fun playingTrack(): PlayingTrack =
    BundleCompat.getParcelable(
      bundle,
      WidgetUpdater.TRACK_INFO,
      PlayingTrack::class.java,
    ) ?: PlayingTrack()

  override fun toString(): String =
    when {
      this.isState() -> "State: ${this.state()}"
      this.isInfo() -> "Info: ${this.playingTrack()}"
      else -> "Unknown"
    }
}
