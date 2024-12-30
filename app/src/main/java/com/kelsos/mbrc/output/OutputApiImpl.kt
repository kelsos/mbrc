package com.kelsos.mbrc.output

import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.networking.ApiBase
import javax.inject.Inject

class OutputApiImpl
  @Inject
  constructor(
    private val apiBase: ApiBase,
  ) : OutputApi {
    override suspend fun getOutputs(): OutputResponse = apiBase.getItem(Protocol.PlayerOutput, OutputResponse::class)

    override suspend fun setOutput(active: String): OutputResponse =
      apiBase.getItem(Protocol.PlayerOutputSwitch, OutputResponse::class, active)
  }
