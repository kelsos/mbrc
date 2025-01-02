package com.kelsos.mbrc.features.widgets

import android.os.Bundle
import androidx.core.os.BundleCompat
import com.kelsos.mbrc.annotations.PlayerState
import com.kelsos.mbrc.features.player.TrackInfo

class BundleData(
  private val bundle: Bundle,
) {
  fun isState() = bundle.getBoolean(WidgetUpdater.STATE, false)

  fun isInfo() = bundle.getBoolean(WidgetUpdater.INFO, false)

  fun isCover() = bundle.getBoolean(WidgetUpdater.COVER, false)

  fun cover(): String = bundle.getString(WidgetUpdater.COVER_PATH, "")

  fun state(): String = bundle.getString(WidgetUpdater.PLAYER_STATE, PlayerState.UNDEFINED)

  fun playingTrack(): TrackInfo =
    BundleCompat.getParcelable(
      bundle,
      WidgetUpdater.TRACK_INFO,
      TrackInfo::class.java,
    ) ?: TrackInfo()

  override fun toString(): String =
    when {
      this.isState() -> "State: ${this.state()}"
      this.isInfo() -> "Info: ${this.playingTrack()}"
      this.isCover() -> "Cover: ${this.cover()}"
      else -> "Unknown"
    }
}
