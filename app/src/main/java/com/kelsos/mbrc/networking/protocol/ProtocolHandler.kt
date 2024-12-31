package com.kelsos.mbrc.networking.protocol

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.kelsos.mbrc.common.state.MainDataModel
import com.kelsos.mbrc.constants.Const
import com.kelsos.mbrc.constants.ProtocolEventType
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtocolHandler
  @Inject
  constructor(
    private val bus: RxBus,
    private val mapper: ObjectMapper,
    private val model: MainDataModel,
  ) {
    private var isHandshakeComplete: Boolean = false

    fun resetHandshake() {
      isHandshakeComplete = false
    }

    fun preProcessIncoming(incoming: String) {
      try {
        val replies =
          incoming
            .split("\r\n".toRegex())
            .dropLastWhile(String::isEmpty)
            .toTypedArray()

        replies.forEach {
          Timber.Forest.v("received:: $it")

          val node = mapper.readValue(it, JsonNode::class.java)
          val context =
            node
              .path("context")
              .textValue()
              .trim()
              .lowercase(Locale.getDefault())

          if (context == Protocol.CLIENT_NOT_ALLOWED) {
            bus.post(MessageEvent(ProtocolEventType.INFORM_CLIENT_NOT_ALLOWED))
            return
          }

          if (!isHandshakeComplete) {
            when (context) {
              Protocol.PLAYER -> bus.post(MessageEvent(ProtocolEventType.INITIATE_PROTOCOL_REQUEST))
              Protocol.PROTOCOL_TAG -> handleProtocolMessage(node)
              else -> return
            }
          }

          bus.post(MessageEvent(context, node.path(Const.DATA)))
        }
      } catch (e: Exception) {
        Timber.Forest.v(e, "Failure while processing incoming data")
      }
    }

    private fun handleProtocolMessage(node: JsonNode) {
      model.pluginProtocol = getProtocolVersion(node)
      if (model.apiOutOfDate) {
        bus.post(MessageEvent(ProtocolEventType.PLUGIN_UPDATE_AVAILABLE))
      }
      isHandshakeComplete = true
      bus.post(MessageEvent(ProtocolEventType.HANDSHAKE_COMPLETE, true))
    }

    private fun getProtocolVersion(node: JsonNode): Int =
      try {
        Integer.parseInt(node.path(Const.DATA).asText())
      } catch (ignore: Exception) {
        2
      }
  }
