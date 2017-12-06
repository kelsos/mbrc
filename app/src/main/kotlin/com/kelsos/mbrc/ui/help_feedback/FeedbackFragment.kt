package com.kelsos.mbrc.ui.help_feedback

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
import androidx.fragment.app.Fragment
import com.kelsos.mbrc.BuildConfig.APPLICATION_ID
import com.kelsos.mbrc.R
import com.kelsos.mbrc.logging.LogHelper
import com.kelsos.mbrc.utilities.RemoteUtils.getVersion
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File

class FeedbackFragment : Fragment() {

  private lateinit var feedbackEditText: EditText
  private lateinit var deviceInfo: CheckBox
  private lateinit var logInfo: CheckBox
  private lateinit var feedbackButton: Button

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_feedback, container, false)
    feedbackEditText = view.findViewById(R.id.feedback_content);
    deviceInfo = view.findViewById(R.id.include_device_info)
    logInfo = view.findViewById(R.id.include_log_info)
    feedbackButton = view.findViewById(R.id.feedback_button)

    feedbackButton.setOnClickListener { onFeedbackButtonClicked() }

    LogHelper.logsExist(requireContext())
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({
        logInfo.isEnabled = true
      }) {

      }
    return view
  }

  private fun onFeedbackButtonClicked() {
    var feedbackText = feedbackEditText.text.toString().trim { it <= ' ' }
    if (TextUtils.isEmpty(feedbackText)) {
      return
    }

    feedbackButton.isEnabled = false

    if (deviceInfo.isChecked) {
      val device = Build.DEVICE
      val manufacturer = Build.MANUFACTURER
      val appVersion = requireContext().getVersion()
      val androidVersion = Build.VERSION.RELEASE

      feedbackText += getString(
        R.string.feedback_version_info,
        manufacturer,
        device,
        androidVersion,
        appVersion
      )
    }

    if (!logInfo.isChecked) {
      openChooser(feedbackText)
      return
    }

    LogHelper.zipLogs(requireContext())
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({
        openChooser(feedbackText, it)
      }) {
        openChooser(feedbackText)
      }
  }

  private fun openChooser(feedbackText: String, logs: File? = null) {
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
    fun newInstance(): FeedbackFragment {
      return FeedbackFragment()
    }
  }
}
