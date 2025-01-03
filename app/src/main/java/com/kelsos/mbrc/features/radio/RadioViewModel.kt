package com.kelsos.mbrc.features.radio

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kelsos.mbrc.common.mvvm.BaseViewModel
import com.kelsos.mbrc.common.mvvm.UiMessageBase
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.features.queue.QueueHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

interface RadioActions {
  fun play(path: String)

  fun reload()
}

sealed class RadioUiMessages : UiMessageBase {
  object QueueFailed : RadioUiMessages()

  object QueueSuccess : RadioUiMessages()

  object RefreshSuccess : RadioUiMessages()

  object RefreshFailed : RadioUiMessages()
}

class RadioViewModel(
  private val radioRepository: RadioRepository,
  private val queueUseCase: QueueHandler,
  private val dispatchers: AppCoroutineDispatchers,
) : BaseViewModel<RadioUiMessages>() {
  val actions: RadioActions =
    object : RadioActions {
      override fun play(path: String) {
        this@RadioViewModel.play(path)
      }

      override fun reload() {
        this@RadioViewModel.reload()
      }
    }

  val radios: Flow<PagingData<RadioStation>> = radioRepository.getAll().cachedIn(viewModelScope)

  fun reload() {
    viewModelScope.launch(dispatchers.network) {
      val result =
        try {
          radioRepository.getRemote()
          RadioUiMessages.RefreshSuccess
        } catch (e: IOException) {
          Timber.e(e)
          RadioUiMessages.RefreshFailed
        }
      emit(result)
    }
  }

  fun play(path: String) {
    viewModelScope.launch(dispatchers.network) {
      val response = queueUseCase.queuePath(path)
      val uiMessage =
        if (response.success) {
          RadioUiMessages.QueueSuccess
        } else {
          RadioUiMessages.QueueFailed
        }
      emit(uiMessage)
    }
  }
}
