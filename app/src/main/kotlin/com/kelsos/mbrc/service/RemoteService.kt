package com.kelsos.mbrc.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kelsos.mbrc.core.networking.ClientConnectionManager
import com.kelsos.mbrc.service.mediasession.AppNotificationManager
import com.kelsos.mbrc.state.AppStateManager
import org.koin.android.ext.android.inject
import timber.log.Timber

class RemoteService : Service() {
  private val receiver: NotificationActionReceiver by inject()
  private val appStateManager: AppStateManager by inject()
  private val notificationManager: AppNotificationManager by inject()
  private val connectionManager: ClientConnectionManager by inject()
  private val handler = Handler(Looper.getMainLooper())
  private var receiverRegistered = false

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    Timber.d("Background Service::Created")
    if (!startForegroundSafely()) {
      stopSelf()
      return
    }
    ServiceState.setRunning(true)
    ServiceState.setStopping(false)
    ContextCompat.registerReceiver(
      this,
      receiver,
      receiver.filter(this),
      ContextCompat.RECEIVER_EXPORTED
    )
    receiverRegistered = true
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Timber.d("Background Service::Started")
    notificationManager.initialize()
    if (!startForegroundSafely()) {
      stopSelf()
      return START_NOT_STICKY
    }
    appStateManager.start()
    connectionManager.start()
    // START_NOT_STICKY: this is a user-facing remote-control service. If the process is killed
    // we must NOT let the framework recreate it in the background, where startForeground() is
    // rejected on Android 12+ (ForegroundServiceStartNotAllowedException). It is started again
    // from the UI when the user reopens the app.
    return START_NOT_STICKY
  }

  /**
   * Promotes the service to the foreground, tolerating Android 12+ rejecting the start while the
   * app is in the background (e.g. a backgrounded start race). [startForeground] throws
   * `ForegroundServiceStartNotAllowedException`, an [IllegalStateException] subclass, in that case;
   * we stop rather than crash. Returns true when the service successfully entered the foreground.
   */
  private fun startForegroundSafely(): Boolean = try {
    startForeground(
      AppNotificationManager.MEDIA_SESSION_NOTIFICATION_ID,
      notificationManager.createPlaceholder()
    )
    true
  } catch (e: IllegalStateException) {
    Timber.w(e, "startForeground rejected (background start on Android 12+); stopping service")
    false
  }

  override fun onDestroy() {
    super.onDestroy()
    notificationManager.destroy()
    appStateManager.stop()
    connectionManager.stop()
    ServiceState.setStopping(true)
    ServiceState.setRunning(false)
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    if (receiverRegistered) {
      unregisterReceiver(receiver)
      receiverRegistered = false
    }
    handler.postDelayed(
      {
        ServiceState.setStopping(false)
        Timber.d("Background Service::Destroyed")
      },
      DESTROY_DELAY_MS
    )
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    super.onTaskRemoved(rootIntent)
    appStateManager.stop()
  }

  companion object {
    const val DESTROY_DELAY_MS = 150L
  }
}
