package com.kelsos.mbrc.platform

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kelsos.mbrc.common.state.AppStateManager
import com.kelsos.mbrc.networking.client.ClientConnectionManager
import com.kelsos.mbrc.platform.mediasession.AppNotificationManager
import org.koin.android.ext.android.inject
import timber.log.Timber

class RemoteService : Service() {
  private val receiver: RemoteBroadcastReceiver by inject()
  private val appStateManager: AppStateManager by inject()
  private val notificationManager: AppNotificationManager by inject()
  private val connectionManager: ClientConnectionManager by inject()
  private lateinit var handler: Handler

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    Timber.d("Background Service::Created")
    startForeground(AppNotificationManager.MEDIA_SESSION_NOTIFICATION_ID, notificationManager.createPlaceholder())
    val looper = requireNotNull(Looper.myLooper())
    handler = Handler(looper)
    serviceRunning = true
    ContextCompat.registerReceiver(this, receiver, receiver.filter(this), ContextCompat.RECEIVER_EXPORTED)
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int,
  ): Int {
    Timber.d("Background Service::Started")
    notificationManager.initialize()
    startForeground(AppNotificationManager.MEDIA_SESSION_NOTIFICATION_ID, notificationManager.createPlaceholder())
    appStateManager.start()
    connectionManager.start()
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()
    notificationManager.destroy()
    appStateManager.stop()
    connectionManager.stop()
    serviceStopping = true
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    this.unregisterReceiver(receiver)
    handler.postDelayed(
      {
        serviceStopping = false
        serviceRunning = false
        Timber.d("Background Service::Destroyed")
      },
      DESTROY_DELAY_MS,
    )
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    super.onTaskRemoved(rootIntent)
    appStateManager.stop()
  }

  companion object {
    var serviceRunning = false
    var serviceStopping = false
    const val DESTROY_DELAY_MS = 150L
  }
}
