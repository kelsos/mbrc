package com.kelsos.mbrc.features.help.compose

import android.content.Intent
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.kelsos.mbrc.BuildConfig.APPLICATION_ID
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.utilities.RemoteUtils
import com.kelsos.mbrc.features.help.FeedbackUiMessage
import com.kelsos.mbrc.features.help.FeedbackViewModel
import java.io.File
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private sealed class HelpFeedbackTab(val index: Int) {
  data object Help : HelpFeedbackTab(0)
  data object Feedback : HelpFeedbackTab(1)
}

@Composable
fun HelpFeedbackScreen(
  modifier: Modifier = Modifier,
  viewModel: FeedbackViewModel = koinViewModel()
) {
  var selectedTab by remember { mutableStateOf<HelpFeedbackTab>(HelpFeedbackTab.Help) }

  Column(modifier = modifier.fillMaxSize()) {
    TabRow(selectedTabIndex = selectedTab.index) {
      Tab(
        selected = selectedTab is HelpFeedbackTab.Help,
        onClick = { selectedTab = HelpFeedbackTab.Help },
        text = { Text(stringResource(R.string.tab_help)) }
      )
      Tab(
        selected = selectedTab is HelpFeedbackTab.Feedback,
        onClick = { selectedTab = HelpFeedbackTab.Feedback },
        text = { Text(stringResource(R.string.common_feedback)) }
      )
    }

    when (selectedTab) {
      is HelpFeedbackTab.Help -> HelpContent(modifier = Modifier.weight(1f))
      is HelpFeedbackTab.Feedback -> FeedbackContent(
        modifier = Modifier.weight(1f),
        viewModel = viewModel
      )
    }
  }
}

@Suppress("COMPOSE_UICOMPOSABLE_INVOCATION")
@Composable
private fun HelpContent(modifier: Modifier = Modifier) {
  LazyColumn(modifier = modifier.fillMaxSize()) {
    item {
      AndroidView(
        factory = { context ->
          WebView(context).apply {
            webViewClient = object : WebViewClient() {
              override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
              ): Boolean {
                val url = request?.url.toString()
                view?.loadUrl(url)
                return false
              }
            }
            loadUrl("https://mbrc.kelsos.net/help?version=${RemoteUtils.VERSION}")
          }
        },
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

@Composable
private fun FeedbackContent(modifier: Modifier = Modifier, viewModel: FeedbackViewModel) {
  val context = LocalContext.current
  var feedbackText by remember { mutableStateOf("") }
  var includeDeviceInfo by remember { mutableStateOf(false) }
  var includeLogInfo by remember { mutableStateOf(false) }
  var isButtonEnabled by remember { mutableStateOf(true) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    viewModel.checkIfLogsExist(context.filesDir)
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is FeedbackUiMessage.UpdateLogsExist -> {
          includeLogInfo = event.logsExist
        }
        is FeedbackUiMessage.ZipFailed -> {
          openFeedbackChooser(context, feedbackText, includeDeviceInfo, null)
          isButtonEnabled = true
        }
        is FeedbackUiMessage.ZipSuccess -> {
          openFeedbackChooser(context, feedbackText, includeDeviceInfo, event.zipFile)
          isButtonEnabled = true
        }
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    OutlinedTextField(
      value = feedbackText,
      onValueChange = { feedbackText = it },
      label = { Text(stringResource(R.string.feedback_title)) },
      modifier = Modifier.fillMaxWidth(),
      minLines = 5
    )

    Column {
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Checkbox(
          checked = includeDeviceInfo,
          onCheckedChange = { includeDeviceInfo = it }
        )
        Text(
          text = stringResource(R.string.feedback_device_information),
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(start = 8.dp)
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Checkbox(
          checked = includeLogInfo,
          onCheckedChange = { includeLogInfo = it }
        )
        Text(
          text = stringResource(R.string.feedback_logs),
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(start = 8.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
      onClick = {
        if (feedbackText.isNotBlank()) {
          isButtonEnabled = false
          if (includeLogInfo) {
            scope.launch {
              viewModel.createZip(
                context.filesDir,
                context.externalCacheDir ?: context.cacheDir
              )
            }
          } else {
            openFeedbackChooser(context, feedbackText, includeDeviceInfo, null)
            isButtonEnabled = true
          }
        }
      },
      enabled = isButtonEnabled && feedbackText.isNotBlank(),
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(stringResource(R.string.feedback_button_text))
    }
  }
}

private fun openFeedbackChooser(
  context: android.content.Context,
  feedbackText: String,
  includeDeviceInfo: Boolean,
  logs: File?
) {
  var fullFeedbackText = feedbackText.trim()

  if (includeDeviceInfo) {
    fullFeedbackText += context.getString(
      R.string.feedback_version_info,
      Build.MANUFACTURER,
      Build.DEVICE,
      Build.VERSION.RELEASE,
      RemoteUtils.VERSION
    )
  }

  val emailIntent = Intent(Intent.ACTION_SEND).apply {
    putExtra(Intent.EXTRA_EMAIL, arrayOf("kelsos@kelsos.net"))
    type = "message/rfc822"
    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_subject))
    putExtra(Intent.EXTRA_TEXT, fullFeedbackText)

    if (logs != null) {
      val logsUri = FileProvider.getUriForFile(
        context,
        "$APPLICATION_ID.fileprovider",
        logs
      )
      putExtra(Intent.EXTRA_STREAM, logsUri)
    }
  }

  context.startActivity(
    Intent.createChooser(
      emailIntent,
      context.getString(R.string.feedback_chooser_title)
    )
  )
}
