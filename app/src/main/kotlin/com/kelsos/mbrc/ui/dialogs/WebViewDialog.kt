package com.kelsos.mbrc.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.webkit.WebView

class WebViewDialog : DialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val context = requireContext()

    return AlertDialog.Builder(context).apply {
      val webView = WebView(context)
      val arguments = checkNotNull(arguments) { "no arguments" }
      webView.loadUrl(arguments.getString(ARG_URL))
      setView(webView)
      setTitle(arguments.getInt(ARG_TITLE))
      setPositiveButton(android.R.string.ok) { dialogInterface, _ -> dialogInterface.dismiss() }
    }.create()
  }

  companion object {
    const val ARG_URL = "url"
    const val ARG_TITLE = "title"
  }
}