package com.kelsos.mbrc.platform

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kelsos.mbrc.R
import com.kelsos.mbrc.constants.UserInputEventType
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.networking.discovery.RemoteServiceDiscovery
import com.kelsos.mbrc.networking.protocol.CommandRegistration
import com.kelsos.mbrc.networking.protocol.RemoteController
import com.kelsos.mbrc.platform.mediasession.RemoteViewIntentBuilder
import com.kelsos.mbrc.platform.mediasession.SessionNotificationManager
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RemoteService : Service() {
  private val controllerBinder = ControllerBinder()

  private val remoteController: RemoteController by inject()
  private val discovery: RemoteServiceDiscovery by inject()
  private val receiver: RemoteBroadcastReceiver by inject()
  private var threadPoolExecutor: ExecutorService? = null
  private lateinit var handler: Handler

  private fun placeholderNotification(): Notification {
    val channel = SessionNotificationManager.Companion.channel()
    channel?.let { notificationChannel ->
      val manager = NotificationManagerCompat.from(this)
      manager.createNotificationChannel(notificationChannel)
    }
    val cancelIntent = RemoteViewIntentBuilder.getPendingIntent(RemoteViewIntentBuilder.CANCEL, this)
    val action =
      NotificationCompat.Action
        .Builder(
          R.drawable.ic_close_black_24dp,
          getString(android.R.string.cancel),
          cancelIntent,
        ).build()

    return NotificationCompat
      .Builder(this, SessionNotificationManager.Companion.CHANNEL_ID)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setSmallIcon(R.drawable.ic_mbrc_status)
      .setContentTitle(getString(R.string.application_name))
      .addAction(action)
      .setContentText(getString(R.string.application_starting))
      .build()
  }

  override fun onBind(intent: Intent?): IBinder = controllerBinder

  override fun onCreate() {
    super.onCreate()
    Timber.Forest.d("Background Service::Created")
    startForeground(SessionNotificationManager.Companion.NOW_PLAYING_PLACEHOLDER, placeholderNotification())
    handler = Handler(Looper.myLooper()!!)
    serviceRunning = true
    ContextCompat.registerReceiver(this, receiver, receiver.filter(this), ContextCompat.RECEIVER_NOT_EXPORTED)
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int,
  ): Int {
    Timber.Forest.d("Background Service::Started")
    startForeground(SessionNotificationManager.Companion.NOW_PLAYING_PLACEHOLDER, placeholderNotification())
    CommandRegistration.register(remoteController, getKoin())
    threadPoolExecutor =
      Executors
        .newSingleThreadExecutor {
          Thread(it, "message-thread")
        }.apply {
          execute(remoteController)
        }

    remoteController.executeCommand(MessageEvent(UserInputEventType.START_CONNECTION))
    discovery.startDiscovery()

    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceStopping = true
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    this.unregisterReceiver(receiver)
    handler.postDelayed({
      remoteController.executeCommand(MessageEvent(UserInputEventType.TERMINATE_CONNECTION))
      CommandRegistration.unregister(remoteController)
      threadPoolExecutor?.shutdownNow()

      serviceStopping = false
      serviceRunning = false
      Timber.Forest.d("Background Service::Destroyed")
    }, 150)
  }

  private inner class ControllerBinder : Binder() {
    val service: ControllerBinder
      @SuppressWarnings("unused")
      get() = this@ControllerBinder
  }

  companion object {
    var serviceRunning = false
    var serviceStopping = false
  }
}
