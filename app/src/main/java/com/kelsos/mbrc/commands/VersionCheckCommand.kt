package com.kelsos.mbrc.commands

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.kelsos.mbrc.constants.ProtocolEventType
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.MainDataModel
import com.kelsos.mbrc.utilities.SettingsManager
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit
import timber.log.Timber
import java.io.IOException
import java.net.URL
import javax.inject.Inject

class VersionCheckCommand
  @Inject
  constructor(
    private val model: MainDataModel,
    private val mapper: ObjectMapper,
    private val manager: SettingsManager,
    private val bus: RxBus,
  ) : ICommand {
    override fun execute(e: IEvent) {
      val now = Instant.now()

      if (check(MINIMUM_REQUIRED)) {
        val next = getNextCheck(true)
        if (next.isAfter(now)) {
          Timber.d("Next update required check is @ $next")
          return
        }
        bus.post(MessageEvent(ProtocolEventType.PluginUpdateRequired))
        model.minimumRequired = MINIMUM_REQUIRED
        model.pluginUpdateRequired = true
        manager.setLastUpdated(now, true)
        return
      }

      if (!manager.isPluginUpdateCheckEnabled()) {
        return
      }

      val nextCheck = getNextCheck()

      if (nextCheck.isAfter(now)) {
        Timber.d("Next update check after @ $nextCheck")
        return
      }

      val jsonNode: JsonNode
      try {
        jsonNode = mapper.readValue(URL(CHECK_URL), JsonNode::class.java)
      } catch (e1: IOException) {
        Timber.d(e1, "While reading json node")
        return
      }

      val expected = jsonNode.path("tag_name").asText().replace("v", "")

      val found = model.pluginVersion
      if (expected != found && check(expected)) {
        model.pluginUpdateAvailable = true
        bus.post(MessageEvent(ProtocolEventType.PluginUpdateAvailable))
      }

      manager.setLastUpdated(now, false)
      Timber.d("Checked for plugin update @ $now. Found: $found expected: $expected")
    }

    private fun getNextCheck(required: Boolean = false): Instant {
      val lastUpdated = manager.getLastUpdated(required)
      val days = if (required) 1L else 2L
      return lastUpdated.plus(days, ChronoUnit.DAYS)
    }

    private fun check(suggestedVersion: String): Boolean {
      val currentVersion = model.pluginVersion.toVersionArray()
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

    companion object {
      private const val CHECK_URL =
        "https://api.github.com/repos/musicbeeremote/plugin/releases/latest"
      private const val MINIMUM_REQUIRED = "1.4.0"
    }
  }

fun String.toVersionArray(): Array<Int> =
  split("\\.".toRegex())
    .dropLastWhile(String::isEmpty)
    .take(3)
    .map { it.toInt() }
    .toTypedArray()
