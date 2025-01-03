package com.kelsos.mbrc.features.output

import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol

interface OutputApi {
  suspend fun getOutputs(): OutputResponse

  suspend fun setOutput(active: String): OutputResponse
}

class OutputApiImpl(
  private val apiBase: ApiBase,
) : OutputApi {
  override suspend fun getOutputs(): OutputResponse = apiBase.getItem(Protocol.PlayerOutput, OutputResponse::class)

  override suspend fun setOutput(active: String): OutputResponse =
    apiBase.getItem(Protocol.PlayerOutputSwitch, OutputResponse::class, active)
}
