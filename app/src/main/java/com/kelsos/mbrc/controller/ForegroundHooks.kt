package com.kelsos.mbrc.controller

import android.app.Notification

interface ForegroundHooks {
  fun start(
    id: Int,
    notification: Notification,
  )

  fun stop()
}
