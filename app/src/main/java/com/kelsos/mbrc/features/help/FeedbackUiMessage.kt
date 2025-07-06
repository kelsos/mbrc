package com.kelsos.mbrc.features.help

import com.kelsos.mbrc.common.mvvm.UiMessageBase
import java.io.File

sealed class FeedbackUiMessage : UiMessageBase {
  class UpdateLogsExist(val logsExist: Boolean) : FeedbackUiMessage()

  class ZipSuccess(val zipFile: File) : FeedbackUiMessage()

  class ZipFailed : FeedbackUiMessage()
}
