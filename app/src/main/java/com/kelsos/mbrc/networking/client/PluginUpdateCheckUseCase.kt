package com.kelsos.mbrc.networking.client

import com.kelsos.mbrc.features.settings.SettingsManager
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber

@JsonClass(generateAdapter = true)
data class GithubRelease(
  @Json(name = "tag_name")
  val tagName: String
)

fun interface PluginUpdateCheckUseCase {
  suspend fun checkIfUpdateNeeded(pluginVersion: String)
}

class PluginUpdateCheckUseCaseImpl(
  private val manager: SettingsManager,
  private val uiMessage: UiMessageQueue,
  moshi: Moshi
) : PluginUpdateCheckUseCase {
  private val adapter = moshi.adapter(GithubRelease::class.java)
  private val client = OkHttpClient()

  override suspend fun checkIfUpdateNeeded(pluginVersion: String) {
    val now = Instant.now()

    if (handleRequiredPluginUpdateCheck(pluginVersion, now)) return

    // Check if plugin updates are enabled using reactive flow
    if (!manager.pluginUpdateCheckFlow.first()) return
    handleOptionalPluginUpdateCheck(now, pluginVersion)
  }

  private suspend fun handleRequiredPluginUpdateCheck(
    pluginVersion: String,
    now: Instant
  ): Boolean {
    if (check(pluginVersion, MINIMUM_REQUIRED)) {
      if (shouldISkipThisCheck(getNextCheck(required = true), now)) return true
      uiMessage.messages.emit(UiMessage.PluginUpdateRequired(pluginVersion, MINIMUM_REQUIRED))
      manager.setLastUpdated(now, required = true)
      return true
    }
    return false
  }

  private suspend fun handleOptionalPluginUpdateCheck(now: Instant, pluginVersion: String) {
    if (shouldISkipThisCheck(getNextCheck(), now)) return

    try {
      checkForTheLatestPluginRelease(pluginVersion, now)
    } catch (e: IOException) {
      Timber.e(e, "While checking for plugin update")
      return
    }
  }

  private fun shouldISkipThisCheck(nextCheck: Instant, now: Instant?): Boolean {
    if (nextCheck.isAfter(now)) {
      Timber.d("Next update check after @ $nextCheck")
      return true
    }
    return false
  }

  private suspend fun checkForTheLatestPluginRelease(pluginVersion: String, now: Instant) {
    val request =
      Request
        .Builder()
        .url(CHECK_URL)
        .build()

    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) throw IOException("Unexpected code $response")

      handlePluginUpdateCheckResponse(response, pluginVersion, now)
    }
  }

  private suspend fun handlePluginUpdateCheckResponse(
    response: Response,
    pluginVersion: String,
    now: Instant
  ) {
    val body = checkNotNull(response.body)
    val versionResponse = adapter.fromJson(body.source())
    val expectedVersion = checkNotNull(versionResponse?.tagName).replace("v", "")

    if (expectedVersion != pluginVersion && check(pluginVersion, expectedVersion)) {
      Timber.d("Checked for plugin update @ $now. Found: $pluginVersion expected: $expectedVersion")
      uiMessage.messages.emit(UiMessage.PluginUpdateAvailable)
    }

    manager.setLastUpdated(now, false)
  }

  private suspend fun getNextCheck(required: Boolean = false): Instant {
    val lastUpdated = manager.getLastUpdated(required)
    val days = if (required) 1L else 2L
    return lastUpdated.plus(days, ChronoUnit.DAYS)
  }

  private fun check(reportedVersion: String, suggestedVersion: String): Boolean {
    val currentVersion = reportedVersion.toVersionArray()
    val latestVersion = suggestedVersion.toVersionArray()

    var i = 0
    val currentSize = currentVersion.size
    val latestSize = latestVersion.size
    while (i < currentSize && i < latestSize && currentVersion[i] == latestVersion[i]) {
      i++
    }

    if (i < currentSize && i < latestSize) {
      val diff = currentVersion[i].compareTo(latestVersion[i])
      return diff < 0
    }

    return false
  }

  private fun String.toVersionArray(): IntArray {
    val parts = split("\\.", limit = 3)
    val versionArray = IntArray(3)
    for (i in parts.indices) {
      versionArray[i] = parts[i].toIntOrNull() ?: 0
    }
    return versionArray
  }

  companion object {
    private const val MINIMUM_REQUIRED = "1.4.0"
    private const val CHECK_URL =
      "https://api.github.com/repos/musicbeeremote/plugin/releases/latest"
  }
}
