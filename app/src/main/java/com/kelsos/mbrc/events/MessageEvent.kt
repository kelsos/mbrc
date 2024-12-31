package com.kelsos.mbrc.events

import com.fasterxml.jackson.databind.node.TextNode
import com.kelsos.mbrc.constants.ProtocolEventType
import com.kelsos.mbrc.data.UserAction
import com.kelsos.mbrc.networking.protocol.ProtocolMessage

data class MessageEvent(
  override var type: String = "",
  override var data: Any = "",
) : ProtocolMessage {
  override val dataString: String
    get() {
      return when (data.javaClass) {
        TextNode::class.java -> (data as TextNode).asText()
        String::class.java -> data as String
        else -> ""
      }
    }

  companion object {
    fun action(data: UserAction): MessageEvent = MessageEvent(ProtocolEventType.USER_ACTION, data)
  }
}
