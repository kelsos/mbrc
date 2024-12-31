package com.kelsos.mbrc.di.providers

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject
import javax.inject.Provider

class NotificationManagerCompatProvider
  @Inject
  constructor(
    private val context: Application,
  ) : Provider<NotificationManagerCompat> {
    override fun get(): NotificationManagerCompat = NotificationManagerCompat.from(context)
  }
