package com.kelsos.mbrc.features.lyrics

import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.LyricsUpdatedEvent

class LyricsModel(
  private val bus: RxBus,
) {
  var lyrics: String = ""
    set(value) {
      if (field == value) {
        return
      }
      field =
        value
          .replace("<p>", "\r\n")
          .replace("<br>", "\n")
          .replace("&lt;", "<")
          .replace("&gt;", ">")
          .replace("&quot;", "\"")
          .replace("&apos;", "'")
          .replace("&amp;", "&")
          .replace("<p>", "\r\n")
          .replace("<br>", "\n")
          .trim { it <= ' ' }

      bus.post(LyricsUpdatedEvent(field))
    }

  var status: Int = LyricsPayload.Companion.NOT_FOUND
}
