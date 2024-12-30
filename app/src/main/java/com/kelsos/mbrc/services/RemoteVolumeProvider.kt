package com.kelsos.mbrc.services

import androidx.media.VolumeProviderCompat
import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.UserAction
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.VolumeChange
import com.kelsos.mbrc.model.MainDataModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteVolumeProvider
@Inject
constructor(
  private val mainDataModel: MainDataModel,
  private val bus: RxBus
) : VolumeProviderCompat(VOLUME_CONTROL_ABSOLUTE, 100, 0) {

  init {
    super.setCurrentVolume(mainDataModel.volume)
    bus.register(this, VolumeChange::class.java) { super.setCurrentVolume(it.volume) }
  }

  override fun onSetVolumeTo(volume: Int) {
    post(UserAction.create(Protocol.PlayerVolume, volume))
    currentVolume = volume
  }

  override fun onAdjustVolume(direction: Int) {
    if (direction == 0) {
      return
    }
    val volume = mainDataModel.volume.plus(direction)
      .coerceAtLeast(0)
      .coerceAtMost(100)
    post(UserAction.create(Protocol.PlayerVolume, volume))
    currentVolume = volume
  }

  private fun post(action: UserAction) {
    bus.post(MessageEvent.action(action))
  }
}
