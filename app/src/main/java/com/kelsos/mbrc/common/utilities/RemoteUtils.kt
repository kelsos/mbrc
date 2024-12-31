package com.kelsos.mbrc.common.utilities

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.pm.PackageInfoCompat
import java.security.MessageDigest

object RemoteUtils {
  @Throws(PackageManager.NameNotFoundException::class)
  fun Context.getVersion(): String? = packageManager.getPackageInfo(packageName, 0).versionName

  @Throws(PackageManager.NameNotFoundException::class)
  fun Context.getVersionCode(): Long = PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0))

  fun coverBitmapSync(coverPath: String): Bitmap? {
    return try {
      val options = BitmapFactory.Options()
      options.inPreferredConfig = Bitmap.Config.RGB_565
      return BitmapFactory.decodeFile(coverPath, options)
    } catch (e: Exception) {
      null
    }
  }

  fun sha1(input: String) = hashString("SHA-1", input)

  private fun hashString(
    type: String,
    input: String,
  ): String {
    val hexChars = "0123456789ABCDEF"
    val bytes =
      MessageDigest
        .getInstance(type)
        .digest(input.toByteArray())
    val result = StringBuilder(bytes.size * 2)

    bytes.forEach {
      val i = it.toInt()
      result.append(hexChars[i shr 4 and 0x0f])
      result.append(hexChars[i and 0x0f])
    }

    return result.toString()
  }
}
