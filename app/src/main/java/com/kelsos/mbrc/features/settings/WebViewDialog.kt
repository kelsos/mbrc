package com.kelsos.mbrc.features.settings

import android.R.string.ok
import android.app.Dialog
import android.os.Bundle
import android.webkit.WebView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class WebViewDialog : DialogFragment() {
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val args = checkNotNull(arguments) { "argument is null" }
    val title = checkNotNull(args.getInt(ARG_TITLE))
    val url = checkNotNull(args.getString(ARG_URL))

    return MaterialAlertDialogBuilder(requireActivity())
      .setTitle(title)
      .setView(
        WebView(requireActivity()).apply {
          loadUrl(url)
        },
      ).setPositiveButton(ok) { dialog, _ -> dialog.dismiss() }
      .show()
  }

  companion object {
    const val ARG_URL = "url"
    const val ARG_TITLE = "title"
  }
}
