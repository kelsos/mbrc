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
  private val backingOutputs = MutableSharedFlow<OutputResponse>()
  private val backingEvents = MutableSharedFlow<OutputSelectionResult>()

  val outputs: SharedFlow<OutputResponse> = backingOutputs
  val events: SharedFlow<OutputSelectionResult> = backingEvents

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
        backingOutputs.emit(outputApi.getOutputs())
      } catch (e: IOException) {
        backingEvents.emit(code(e))
        Timber.e(e)
      }
    }
  }

  fun setOutput(output: String) {
    viewModelScope.launch {
      try {
        backingOutputs.emit(outputApi.setOutput(output))
      } catch (e: IOException) {
        backingEvents.emit(code(e))
      }
    }
  }
}
