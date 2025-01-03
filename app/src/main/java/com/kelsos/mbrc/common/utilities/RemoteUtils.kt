package com.kelsos.mbrc.common.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kelsos.mbrc.BuildConfig

object RemoteUtils {
  const val VERSION: String = BuildConfig.VERSION_NAME
  const val VERSION_CODE: Int = BuildConfig.VERSION_CODE

  fun loadBitmap(path: String): Result<Bitmap> =
    runCatching {
      BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply {
          inPreferredConfig = Bitmap.Config.RGB_565
        },
      )
    }
}
