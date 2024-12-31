package com.kelsos.mbrc.features.output

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.inject.Inject

class OutputSelectionViewModel
  @Inject
  constructor(
    private val outputApi: OutputApi,
    dispatchers: AppCoroutineDispatchers,
  ) : ViewModel() {
    private val job = SupervisorJob()
    private val context = job + dispatchers.main
    private val scope = CoroutineScope(context)
    private val _outputs: MutableLiveData<List<String>> = MutableLiveData()
    private val _selection: MutableLiveData<String> = MutableLiveData()
    private val _events: MutableLiveData<OutputSelectionResult> = MutableLiveData()

    init {
      _outputs.postValue(emptyList())
      _selection.postValue("")
    }

    val outputs: LiveData<List<String>>
      get() = _outputs

    val selection: LiveData<String>
      get() = _selection

    val events: LiveData<OutputSelectionResult>
      get() = _events

    private fun updateState(response: OutputResponse) {
      _outputs.postValue(response.devices)
      _selection.postValue(response.active)
    }

    private fun code(throwable: Throwable?): OutputSelectionResult =
      when (throwable?.cause ?: throwable) {
        is SocketException -> OutputSelectionResult.ConnectionError
        is SocketTimeoutException -> OutputSelectionResult.ConnectionError
        else -> OutputSelectionResult.UnknownError
      }

    fun reload() {
      scope.launch {
        try {
          val response = outputApi.getOutputs()
          updateState(response)
          _events.postValue(OutputSelectionResult.Success)
        } catch (e: Exception) {
          Timber.Forest.e(e)
          _events.postValue(code(e))
        }
      }
    }

    fun setOutput(output: String) {
      scope.launch {
        try {
          val response = outputApi.setOutput(output)
          updateState(response)
          _events.postValue(OutputSelectionResult.Success)
        } catch (e: Exception) {
          _events.postValue(code(e))
        }
      }
    }

    override fun onCleared() {
      job.cancel()
      super.onCleared()
    }
  }
