package com.kelsos.mbrc.features.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.utilities.RemoteUtils

class HelpFragment : Fragment() {
  private lateinit var helpView: WebView

  override fun onStart() {
    super.onStart()
    helpView.loadUrl("https://mbrc.kelsos.net/help?version=${RemoteUtils.VERSION}")
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    // Inflate the layout for this fragment
    val view = inflater.inflate(R.layout.fragment_help, container, false)
    helpView = view.findViewById(R.id.help_webview)
    helpView.webViewClient = RemoteWebViewClient()
    return view
  }

  private class RemoteWebViewClient : WebViewClient() {
    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(
      view: WebView,
      url: String,
    ): Boolean {
      view.loadUrl(url)
      return false
    }

    override fun shouldOverrideUrlLoading(
      view: WebView?,
      request: WebResourceRequest?,
    ): Boolean {
      val url = request?.url.toString()
      view?.loadUrl(url)
      return false
    }
  }

  companion object {
    fun newInstance(): HelpFragment = HelpFragment()
  }
}
