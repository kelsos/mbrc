package com.kelsos.mbrc.platform

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.kelsos.mbrc.constants.UserInputEventType
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.networking.MulticastConfigurationDiscovery
import com.kelsos.mbrc.networking.protocol.CommandExecutor
import com.kelsos.mbrc.networking.protocol.CommandRegistration
import com.kelsos.mbrc.platform.media_session.SessionNotificationManager
import timber.log.Timber
import toothpick.Scope
import toothpick.Toothpick
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteService : Service(), ForegroundHooks {

  private val controllerBinder = ControllerBinder()
  @Inject lateinit var commandExecutor: CommandExecutor
  @Inject lateinit var discovery: MulticastConfigurationDiscovery
  @Inject lateinit var receiver: RemoteBroadcastReceiver
  @Inject lateinit var sessionNotificationManager: SessionNotificationManager

  private var threadPoolExecutor: ExecutorService? = null
  private var scope: Scope? = null

  override fun onBind(intent: Intent?): IBinder {
    return controllerBinder
  }

  override fun onCreate() {
    super.onCreate()
    scope = Toothpick.openScope(application)
    Toothpick.inject(this, scope)
    this.registerReceiver(receiver, receiver.filter())
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Timber.d("Background Service::Started")
    sessionNotificationManager.setForegroundHooks(this)
    CommandRegistration.register(commandExecutor, scope!!)
    threadPoolExecutor = Executors.newSingleThreadExecutor { Thread(it, "message-thread")}
    threadPoolExecutor!!.execute(commandExecutor)
    commandExecutor.executeCommand(MessageEvent(UserInputEventType.StartConnection))
    discovery.startDiscovery {  }

    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()
    this.unregisterReceiver(receiver)
    commandExecutor.executeCommand(MessageEvent(UserInputEventType.CancelNotification))
    commandExecutor.executeCommand(MessageEvent(UserInputEventType.TerminateConnection))
    CommandRegistration.unregister(commandExecutor)
    threadPoolExecutor?.shutdownNow()
    Timber.d("Background Service::Destroyed")
    Toothpick.closeScope(this)
  }

  override fun start(id: Int, notification: Notification) {
    Timber.v("Notification is starting foreground")
    startForeground(id, notification)
  }

  override fun stop() {
    Timber.v("Notification is stopping foreground")
    stopForeground(true)
  }

  private inner class ControllerBinder : Binder() {
    internal val service: ControllerBinder
      @SuppressWarnings("unused")
      get() = this@ControllerBinder
  }
}
