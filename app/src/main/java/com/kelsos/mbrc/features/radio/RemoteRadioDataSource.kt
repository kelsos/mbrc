package com.kelsos.mbrc.features.radio

import com.kelsos.mbrc.common.data.RemoteDataSource
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.flow.Flow

class RemoteRadioDataSource(
  private val service: ApiBase,
) : RemoteDataSource<RadioStation> {
  override suspend fun fetch(): Flow<List<RadioStation>> = service.getAllPages(Protocol.RADIO_STATIONS, RadioStation::class)
}
