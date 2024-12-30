package com.kelsos.mbrc.commands

import com.kelsos.mbrc.data.SocketMessage
import com.kelsos.mbrc.data.UserAction
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.services.SocketService
import javax.inject.Inject

class ProcessUserAction
  @Inject
  constructor(
    private val socket: SocketService,
  ) : ICommand {
    override fun execute(e: IEvent) {
      socket.sendData(
        SocketMessage.create(
          (e.data as UserAction).context,
          (e.data as UserAction).data,
        ),
      )
    }
  }
