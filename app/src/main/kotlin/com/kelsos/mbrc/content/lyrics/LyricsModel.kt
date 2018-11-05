package com.kelsos.mbrc.content.lyrics

import com.kelsos.mbrc.events.LyricsUpdatedEvent
import com.kelsos.mbrc.events.bus.RxBus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsModel
@Inject
constructor(private val bus: RxBus) {
  var lyrics: String = ""
    set(value) {
      if (field == value) {
        return
      }
      field = value.replace("<p>", "\r\n")
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

  var status: Int = LyricsPayload.NOT_FOUND
}