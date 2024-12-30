package com.kelsos.mbrc.di.providers

import android.app.Application
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject
import javax.inject.Provider

class NotificationManagerCompatProvider
@Inject
constructor(context: Application) : Provider<NotificationManagerCompat> {
  private val context: Context

  init {
    this.context = context
  }

  override fun get(): NotificationManagerCompat {
    return NotificationManagerCompat.from(context)
  }
}
