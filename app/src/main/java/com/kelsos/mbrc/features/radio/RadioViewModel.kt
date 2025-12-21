package com.kelsos.mbrc.features.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.features.queue.QueueHandler
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

interface IRadioActions {
  val play: (path: String) -> Unit
  val reload: () -> Unit
}

class RadioActions(
  private val radioRepository: RadioRepository,
  private val queueUseCase: QueueHandler,
  private val connectionStateFlow: ConnectionStateFlow,
  viewModelScope: CoroutineScope,
  dispatchers: AppCoroutineDispatchers,
  events: MutableSharedFlow<RadioUiMessages>
) : IRadioActions {
  override val reload: () -> Unit = {
    viewModelScope.launch(dispatchers.network) {
      val result =
        if (!connectionStateFlow.isConnected()) {
          RadioUiMessages.NetworkUnavailable
        } else {
          try {
            radioRepository.getRemote()
            RadioUiMessages.RefreshSuccess
          } catch (e: IOException) {
            Timber.e(e)
            RadioUiMessages.RefreshFailed
          }
        }
      withContext(dispatchers.main) {
        events.emit(result)
      }
    }
  }

  override val play: (path: String) -> Unit = { path ->
    viewModelScope.launch(dispatchers.network) {
      val uiMessage =
        if (!connectionStateFlow.isConnected()) {
          RadioUiMessages.NetworkUnavailable
        } else {
          val response = queueUseCase.queuePath(path)
          if (response.success) {
            RadioUiMessages.QueueSuccess
          } else {
            RadioUiMessages.QueueFailed
          }
        }
      withContext(dispatchers.main) {
        events.emit(uiMessage)
      }
    }
  }
}

sealed class RadioUiMessages {
  object QueueFailed : RadioUiMessages()

  object QueueSuccess : RadioUiMessages()

  object RefreshSuccess : RadioUiMessages()

  object RefreshFailed : RadioUiMessages()

  object NetworkUnavailable : RadioUiMessages()
}

data class RadioState(val events: Flow<RadioUiMessages>, val radios: Flow<PagingData<RadioStation>>)

class RadioViewModel(
  radioRepository: RadioRepository,
  queueUseCase: QueueHandler,
  connectionStateFlow: ConnectionStateFlow,
  dispatchers: AppCoroutineDispatchers
) : ViewModel() {
  private val events: MutableSharedFlow<RadioUiMessages> = MutableSharedFlow()

  val actions: IRadioActions =
    RadioActions(
      radioRepository,
      queueUseCase,
      connectionStateFlow,
      viewModelScope,
      dispatchers,
      events
    )

  val state =
    RadioState(
      events = events,
      radios = radioRepository.getAll().cachedIn(viewModelScope)
    )
}
