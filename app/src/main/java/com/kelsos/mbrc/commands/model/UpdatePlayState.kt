package com.kelsos.mbrc.commands.model

import android.app.Application
import com.kelsos.mbrc.annotations.PlayerState
import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.PlayStateChange
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.MainDataModel
import com.kelsos.mbrc.widgets.UpdateWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import javax.inject.Inject

class UpdatePlayState
@Inject
constructor(
  private val model: MainDataModel,
  private val context: Application,
  private val bus: RxBus,
  dispatchers: AppDispatchers
) : ICommand {

  private val job = SupervisorJob()
  private val scope = CoroutineScope(job + dispatchers.io)
  private var action: Deferred<Unit>? = null

  override fun execute(e: IEvent) {

    model.playState = e.dataString
    if (model.playState != PlayerState.STOPPED) {
      bus.post(PlayStateChange(model.playState, model.position))
    } else {
      stop()
    }

    UpdateWidgets.updatePlaystate(context, e.dataString)
  }

  private fun stop() {
    action?.cancel()
    action = scope.async {
      delay(800)
      bus.post(PlayStateChange(model.playState, model.position))
    }
  }
}
