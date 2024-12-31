package com.kelsos.mbrc.features.help

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.utilities.RemoteUtils.getVersion
import timber.log.Timber

class HelpFragment : Fragment() {
  private lateinit var helpView: WebView

  override fun onStart() {
    super.onStart()
    val url: String =
      try {
        String.format("https://mbrc.kelsos.net/help?version=%s", requireContext().getVersion())
      } catch (e: PackageManager.NameNotFoundException) {
        Timber.v(e, "Failed to get version")
        "https://mbrc.kelsos.net/help"
      }

    helpView.loadUrl(url)
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
    @Suppress("OverridingDeprecatedMember")
    override fun shouldOverrideUrlLoading(
      view: WebView,
      url: String,
    ): Boolean {
      view.loadUrl(url)
      return false
    }
  }

  companion object {
    fun newInstance(): HelpFragment = HelpFragment()
  }
}
