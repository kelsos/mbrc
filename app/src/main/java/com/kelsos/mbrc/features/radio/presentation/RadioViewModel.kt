package com.kelsos.mbrc.features.radio.presentation

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.kelsos.mbrc.common.utilities.AppDispatchers
import com.kelsos.mbrc.common.utilities.paged
import com.kelsos.mbrc.features.queue.Queue
import com.kelsos.mbrc.features.queue.QueueApi
import com.kelsos.mbrc.features.radio.domain.RadioStation
import com.kelsos.mbrc.features.radio.repository.RadioRepository
import com.kelsos.mbrc.ui.BaseViewModel
import kotlinx.coroutines.launch

class RadioViewModel(
  private val radioRepository: RadioRepository,
  private val queueApi: QueueApi,
  private val dispatchers: AppDispatchers
) : BaseViewModel<RadioUiMessages>(dispatchers) {

  val radios: LiveData<PagedList<RadioStation>> = radioRepository.getAll().paged()

  fun reload() {
    scope.launch(dispatchers.network) {
      val result = radioRepository.getRemote()
        .fold(
          {
            RadioUiMessages.RefreshFailed
          },
          {
            RadioUiMessages.RefreshSuccess
          }
        )
      emit(result)
    }
  }

  fun play(path: String) {
    scope.launch(dispatchers.network) {
      val response = queueApi.queue(Queue.Now, listOf(path))
        .fold(
          {
            RadioUiMessages.NetworkError
          },
          { response ->
            if (response.code == 200) {
              RadioUiMessages.QueueSuccess
            } else {
              RadioUiMessages.QueueFailed
            }
          }
        )

      emit(response)
    }
  }
}
