package com.kelsos.mbrc.common.mvvm

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

interface UiMessageBase

open class BaseViewModel<T : UiMessageBase> : ViewModel() {
  private val backingEvents = MutableSharedFlow<T>()
  val events: Flow<T> = backingEvents

  protected suspend fun emit(uiMessage: T) {
    backingEvents.emit(uiMessage)
  }
}
