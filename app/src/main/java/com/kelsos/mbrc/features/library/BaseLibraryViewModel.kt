package com.kelsos.mbrc.features.library

import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.common.mvvm.BaseViewModel
import com.kelsos.mbrc.common.mvvm.UiMessageBase
import com.kelsos.mbrc.features.queue.Queue
import com.kelsos.mbrc.features.settings.BasicSettingsHelper
import kotlinx.coroutines.launch

open class BaseLibraryViewModel<T : UiMessageBase>(
  private val settingsHelper: BasicSettingsHelper,
) : BaseViewModel<T>() {
  protected fun getQueueAction(action: Queue): Queue =
    if (action == Queue.Default) {
      Queue.fromString(settingsHelper.defaultAction)
    } else {
      action
    }

  protected fun launchDefault(event: T) {
    viewModelScope.launch {
      emit(event)
    }
  }
}
