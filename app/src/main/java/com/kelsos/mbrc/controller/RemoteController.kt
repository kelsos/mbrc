package com.kelsos.mbrc.controller

import android.app.Application
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import timber.log.Timber
import toothpick.Scope
import toothpick.Toothpick
import java.util.HashMap
import java.util.concurrent.LinkedBlockingQueue
import javax.inject.Inject

class RemoteController
  @Inject
  constructor(
    bus: RxBus,
    app: Application,
  ) : Runnable {
    private val scope: Scope
    private var commandMap: MutableMap<String, ICommand>
    private val eventQueue: LinkedBlockingQueue<IEvent> = LinkedBlockingQueue<IEvent>()

    init {
      bus.register(this, MessageEvent::class.java) { this.handleUserActionEvents(it) }
      scope = Toothpick.openScope(app)
      commandMap = HashMap<String, ICommand>()
    }

    fun register(
      type: String,
      command: ICommand,
    ) {
      if (!commandMap.containsKey(type)) {
        commandMap[type] = command
      }
    }

    fun unregister(
      type: String,
      command: Class<out ICommand>,
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
    internal fun executeCommand(event: IEvent) {
      val command = commandMap[event.type] ?: return

      try {
        command.execute(event)
      } catch (ex: Exception) {
        Timber.d(ex, "executing command for type: \t%s", event)
      }
    }

    override fun run() {
      try {
        //noinspection InfiniteLoopStatement
        while (true) {
          executeCommand(eventQueue.take())
        }
      } catch (e: InterruptedException) {
        Timber.d("Command execution was interrupted")
      }
    }
  }
