package com.kelsos.mbrc.controller

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
import com.kelsos.mbrc.configuration.CommandRegistration
import com.kelsos.mbrc.constants.UserInputEventType
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.messaging.NotificationService
import com.kelsos.mbrc.messaging.NotificationService.Companion.CHANNEL_ID
import com.kelsos.mbrc.messaging.NotificationService.Companion.NOW_PLAYING_PLACEHOLDER
import com.kelsos.mbrc.messaging.NotificationService.Companion.channel
import com.kelsos.mbrc.services.ServiceDiscovery
import com.kelsos.mbrc.utilities.RemoteBroadcastReceiver
import com.kelsos.mbrc.utilities.RemoteViewIntentBuilder
import com.kelsos.mbrc.utilities.RemoteViewIntentBuilder.getPendingIntent
import timber.log.Timber
import toothpick.Scope
import toothpick.Toothpick
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteService : Service() {

  private val controllerBinder = ControllerBinder()

  @Inject
  lateinit var remoteController: RemoteController

  @Inject
  lateinit var discovery: ServiceDiscovery

  @Inject
  lateinit var receiver: RemoteBroadcastReceiver

  @Inject
  lateinit var notificationService: NotificationService

  private var threadPoolExecutor: ExecutorService? = null
  private lateinit var scope: Scope
  private lateinit var handler: Handler

  private fun placeholderNotification(): Notification {
    val channel = channel()
    channel?.let { notificationChannel ->
      val manager = NotificationManagerCompat.from(this)
      manager.createNotificationChannel(notificationChannel)
    }
    val cancelIntent = getPendingIntent(RemoteViewIntentBuilder.CANCEL,this)
    val action = NotificationCompat.Action.Builder(
      R.drawable.ic_close_black_24dp,
      getString(android.R.string.cancel),
      cancelIntent
    ).build()

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setSmallIcon(R.drawable.ic_mbrc_status)
      .setContentTitle(getString(R.string.application_name))
      .addAction(action)
      .setContentText(getString(R.string.application_starting))
      .build()
  }

  override fun onBind(intent: Intent?): IBinder {
    return controllerBinder
  }

  override fun onCreate() {
    super.onCreate()
    Timber.d("Background Service::Created")
    startForeground(NOW_PLAYING_PLACEHOLDER, placeholderNotification())
    handler = Handler(Looper.myLooper()!!)
    SERVICE_RUNNING = true
    scope = Toothpick.openScope(application)
    Toothpick.inject(this, scope)
    ContextCompat.registerReceiver(this, receiver, receiver.filter(this), ContextCompat.RECEIVER_NOT_EXPORTED)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Timber.d("Background Service::Started")
    startForeground(NOW_PLAYING_PLACEHOLDER, placeholderNotification())
    CommandRegistration.register(remoteController, scope)
    threadPoolExecutor = Executors.newSingleThreadExecutor {
      Thread(it, "message-thread")
    }.apply {
      execute(remoteController)
    }

    remoteController.executeCommand(MessageEvent(UserInputEventType.StartConnection))
    discovery.startDiscovery()

    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()
    SERVICE_STOPPING = true;
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    this.unregisterReceiver(receiver)
    handler.postDelayed({
      remoteController.executeCommand(MessageEvent(UserInputEventType.TerminateConnection))
      CommandRegistration.unregister(remoteController)
      threadPoolExecutor?.shutdownNow()
      Toothpick.closeScope(this)

      SERVICE_STOPPING = false
      SERVICE_RUNNING = false
      Timber.d("Background Service::Destroyed")
    }, 150)
  }

  private inner class ControllerBinder : Binder() {
    val service: ControllerBinder
      @SuppressWarnings("unused")
      get() = this@ControllerBinder
  }

  companion object {
    var SERVICE_RUNNING = false
    var SERVICE_STOPPING = false
  }
}
