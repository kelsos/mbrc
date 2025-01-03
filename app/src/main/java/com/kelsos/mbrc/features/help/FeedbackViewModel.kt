package com.kelsos.mbrc.features.help

import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.common.mvvm.BaseViewModel
import com.kelsos.mbrc.logging.LogHelper
import kotlinx.coroutines.launch
import java.io.File

class FeedbackViewModel(
  private val logHelper: LogHelper,
) : BaseViewModel<FeedbackUiMessage>() {
  fun checkIfLogsExist(filesDir: File) {
    viewModelScope.launch {
      val logsExist = logHelper.logsExist(filesDir = filesDir)
      emit(FeedbackUiMessage.UpdateLogsExist(logsExist))
    }
  }

  fun createZip(
    filesDir: File,
    externalCacheDir: File,
  ) {
    viewModelScope.launch {
      val result =
        runCatching {
          logHelper.zipLogs(filesDir, externalCacheDir)
        }

      val message =
        if (result.isSuccess) {
          FeedbackUiMessage.ZipSuccess(result.getOrThrow())
        } else {
          FeedbackUiMessage.ZipFailed()
        }

      emit(message)
    }
  }
}
