package com.kelsos.mbrc.features.output

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okio.IOException
import timber.log.Timber
import java.net.SocketException
import java.net.SocketTimeoutException

class OutputSelectionViewModel(
  private val outputApi: OutputApi,
  private val dispatchers: AppCoroutineDispatchers,
) : ViewModel() {
  val outputs: SharedFlow<OutputResponse>
    field = MutableSharedFlow()
  val events: SharedFlow<OutputSelectionResult>
    field = MutableSharedFlow()

  private fun code(throwable: Throwable?): OutputSelectionResult =
    when (throwable?.cause ?: throwable) {
      is SocketException -> OutputSelectionResult.ConnectionError
      is SocketTimeoutException -> OutputSelectionResult.ConnectionError
      else -> OutputSelectionResult.UnknownError
    }

  fun reload() {
    Timber.d("call reload outputs")
    viewModelScope.launch(dispatchers.network) {
      Timber.d("reload outputs")
      try {
        outputs.emit(outputApi.getOutputs())
      } catch (e: IOException) {
        events.emit(code(e))
        Timber.Forest.e(e)
      }
    }
  }

  fun setOutput(output: String) {
    viewModelScope.launch {
      try {
        outputs.emit(outputApi.setOutput(output))
      } catch (e: IOException) {
        events.emit(code(e))
      }
    }
  }
}
