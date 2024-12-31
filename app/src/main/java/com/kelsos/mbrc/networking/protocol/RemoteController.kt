package com.kelsos.mbrc.networking.protocol

import android.app.Application
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import timber.log.Timber
import toothpick.Scope
import toothpick.Toothpick
import java.util.concurrent.LinkedBlockingQueue
import javax.inject.Inject

class RemoteController
  @Inject
  constructor(
    bus: RxBus,
    app: Application,
  ) : Runnable {
    private val scope: Scope
    private var commandMap: MutableMap<String, ProtocolAction>
    private val eventQueue: LinkedBlockingQueue<ProtocolMessage> = LinkedBlockingQueue<ProtocolMessage>()

    init {
      bus.register(this, MessageEvent::class.java) { this.handleUserActionEvents(it) }
      scope = Toothpick.openScope(app)
      commandMap = HashMap<String, ProtocolAction>()
    }

    fun register(
      type: String,
      command: ProtocolAction,
    ) {
      if (!commandMap.containsKey(type)) {
        commandMap[type] = command
      }
    }

    fun unregister(
      type: String,
      command: Class<out ProtocolAction>,
    ) {
      if (commandMap.containsKey(type) && commandMap[type] == command) {
        commandMap.remove(type)
      }
    }

    fun clearCommands() {
      commandMap.clear()
    }

    /**
     * Takes a MessageEvent and passes it to the command execution function.
     * @param event The message received.
     */

    private fun handleUserActionEvents(event: MessageEvent) {
      eventQueue.add(event)
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    internal fun executeCommand(event: ProtocolMessage) {
      val command = commandMap[event.type] ?: return

      try {
        command.execute(event)
      } catch (ex: Exception) {
        Timber.Forest.d(ex, "executing command for type: \t%s", event)
      }
    }

    override fun run() {
      try {
        //noinspection InfiniteLoopStatement
        while (true) {
          executeCommand(eventQueue.take())
        }
      } catch (e: InterruptedException) {
        Timber.Forest.d("Command execution was interrupted")
      }
    }
  }
