package com.kelsos.mbrc.ui.help_feedback

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.kelsos.mbrc.R
import com.kelsos.mbrc.logging.LogHelper
import com.kelsos.mbrc.utilities.RemoteUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File

class FeedbackFragment : Fragment() {

  @BindView(R.id.feedback_content) lateinit var feedbackEditText: EditText
  @BindView(R.id.include_device_info) lateinit var deviceInfo: CheckBox
  @BindView(R.id.include_log_info) lateinit var logInfo: CheckBox
  @BindView(R.id.feedback_button) lateinit var feedbackButton: Button

  override fun onCreateView(inflater: LayoutInflater?,
                            container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    val view = inflater!!.inflate(R.layout.fragment_feedback, container, false)
    ButterKnife.bind(this, view)

    LogHelper.logsExist(context)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
          logInfo.isEnabled = true
        }) {

        }
    return view
  }

  @OnClick(R.id.feedback_button)
  internal fun onFeedbackButtonClicked() {
    var feedbackText = feedbackEditText.text.toString().trim { it <= ' ' }
    if (TextUtils.isEmpty(feedbackText)) {
      return
    }

    feedbackButton.isEnabled = false

    if (deviceInfo.isChecked) {
      val device = Build.DEVICE
      val manufacturer = Build.MANUFACTURER
      val appVersion = RemoteUtils.getVersion(context)
      val androidVersion = Build.VERSION.RELEASE

      feedbackText += getString(R.string.feedback_version_info,
          manufacturer,
          device,
          androidVersion,
          appVersion)
    }

    if (!logInfo.isChecked) {
      openChooser(feedbackText)
      return
    }

    LogHelper.zipLogs(context)
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
      val logsUri = Uri.fromFile(logs)
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
