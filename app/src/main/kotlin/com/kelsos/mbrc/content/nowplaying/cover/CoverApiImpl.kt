package com.kelsos.mbrc.content.nowplaying.cover

import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import io.reactivex.Single
import javax.inject.Inject

class CoverApiImpl
@Inject constructor(
  private val apiBase: ApiBase
) : CoverApi {
  override fun getCover(): Single<String> {
    return apiBase.getItem(Protocol.NowPlayingCover, CoverPayload::class.java).map { payload ->
      if (payload.status == CoverPayload.SUCCESS) {
        return@map payload.cover
      } else {
        throw RuntimeException("Cover not available")
      }
    }
  }
}