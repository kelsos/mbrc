package com.kelsos.mbrc.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.webkit.WebView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class WebViewDialog : DialogFragment() {
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val title = arguments?.getInt(ARG_TITLE) ?: throw Exception("argument null")
    val url = arguments?.getString(ARG_URL) ?: throw Exception("argument null")

    return MaterialAlertDialogBuilder(requireActivity())
      .setTitle(title)
      .setView(
        WebView(requireActivity()).apply {
          loadUrl(url)
        },
      ).setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
      .show()
  }

  companion object {
    const val ARG_URL = "url"
    const val ARG_TITLE = "title"
  }
}
