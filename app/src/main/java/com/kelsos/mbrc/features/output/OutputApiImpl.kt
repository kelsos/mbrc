package com.kelsos.mbrc.features.output

import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol

class OutputApiImpl(
  private val apiBase: ApiBase,
) : OutputApi {
  override suspend fun getOutputs(): OutputResponse = apiBase.getItem(Protocol.PLAYER_OUTPUT, OutputResponse::class)

  override suspend fun setOutput(active: String): OutputResponse =
    apiBase.getItem(Protocol.PLAYER_OUTPUT_SWITCH, OutputResponse::class, active)
}
