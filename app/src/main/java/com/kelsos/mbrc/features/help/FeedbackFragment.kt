package com.kelsos.mbrc.features.help

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kelsos.mbrc.BuildConfig.APPLICATION_ID
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.utilities.RemoteUtils
import kotlinx.coroutines.launch
import org.koin.androidx.scope.ScopeFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class FeedbackFragment : ScopeFragment() {
  private lateinit var feedbackEditText: EditText
  private lateinit var deviceInfo: CheckBox
  private lateinit var logInfo: CheckBox
  private lateinit var feedbackButton: Button

  private val viewModel: FeedbackViewModel by viewModel()

  private val feedbackText: String
    get() {
      var feedbackText = feedbackEditText.text.toString().trim()
      if (deviceInfo.isChecked) {

        feedbackText +=
          getString(
            R.string.feedback_version_info,
            Build.MANUFACTURER,
            Build.DEVICE,
            Build.VERSION.RELEASE,
            RemoteUtils.VERSION,
          )
      }
      return feedbackText
    }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    val view = inflater.inflate(R.layout.fragment_feedback, container, false)
    feedbackEditText = view.findViewById(R.id.feedback_content)
    deviceInfo = view.findViewById(R.id.include_device_info)
    logInfo = view.findViewById(R.id.include_log_info)
    feedbackButton = view.findViewById(R.id.feedback_button)

    feedbackButton.setOnClickListener { onFeedbackButtonClicked() }

    lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
          when (event) {
            is FeedbackUiMessage.UpdateLogsExist -> logInfo.isChecked = event.logsExist
            is FeedbackUiMessage.ZipFailed -> openChooser(feedbackText)
            is FeedbackUiMessage.ZipSuccess -> openChooser(feedbackText, event.zipFile)
          }
        }
      }
    }
    viewModel.checkIfLogsExist(requireContext().filesDir)
    return view
  }

  private fun onFeedbackButtonClicked() {
    if (TextUtils.isEmpty(feedbackText)) {
      return
    }

    feedbackButton.isEnabled = false

    if (!logInfo.isChecked) {
      openChooser(feedbackText)
      return
    }

    lifecycleScope.launch {
      val ctx = requireContext()
      viewModel.createZip(ctx.filesDir, ctx.externalCacheDir ?: ctx.cacheDir)
    }
  }

  private fun openChooser(
    feedbackText: String,
    logs: File? = null,
  ) {
    val emailIntent = Intent(Intent.ACTION_SEND)
    emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("kelsos@kelsos.net"))
    emailIntent.type = "message/rfc822"
    emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
    emailIntent.putExtra(Intent.EXTRA_TEXT, feedbackText)
    if (logs != null) {
      val logsUri = FileProvider.getUriForFile(requireContext(), "$APPLICATION_ID.fileprovider", logs)
      emailIntent.putExtra(Intent.EXTRA_STREAM, logsUri)
    }

    feedbackButton.isEnabled = true

    startActivity(Intent.createChooser(emailIntent, getString(R.string.feedback_chooser_title)))
  }

  companion object {
    fun newInstance(): FeedbackFragment = FeedbackFragment()
  }
}
