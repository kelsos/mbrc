package com.kelsos.mbrc.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kelsos.mbrc.common.utilities.AppDispatchers
import com.kelsos.mbrc.events.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

open class BaseViewModel<T : UiMessageBase>(dispatchers: AppDispatchers) : ViewModel() {
  private val mutableEmitter: MutableLiveData<Event<T>> = MutableLiveData()
  private val job: Job = Job()
  protected val scope = CoroutineScope(dispatchers.main + job)

  val emitter: LiveData<Event<T>>
    get() = mutableEmitter

  protected fun emit(uiMessage: T) {
    mutableEmitter.postValue(Event(uiMessage))
  }

  override fun onCleared() {
    job.cancel()
    super.onCleared()
  }
}
