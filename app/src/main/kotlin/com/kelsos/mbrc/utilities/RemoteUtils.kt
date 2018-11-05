package com.kelsos.mbrc.utilities

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.reactivex.Observable
import java.io.File

object RemoteUtils {

  @Throws(PackageManager.NameNotFoundException::class)
  fun getVersion(mContext: Context): String {
    val mInfo = mContext.packageManager.getPackageInfo(mContext.packageName, 0)
    return mInfo.versionName
  }

  @Throws(PackageManager.NameNotFoundException::class)
  fun getVersionCode(mContext: Context): Long {
    val mInfo = mContext.packageManager.getPackageInfo(mContext.packageName, 0)
    return mInfo.versionCode.toLong()
  }

  fun bitmapFromFile(path: String): Observable<Bitmap> {
    return Observable.create<Bitmap>({
      try {
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.RGB_565
        val bitmap = BitmapFactory.decodeFile(path, options)
        if (bitmap != null) {
          it.onNext(bitmap)
          it.onComplete()
        } else {
          it.onError(RuntimeException("Unable to decode the image"))
        }
      } catch (e: Exception) {
        it.onError(e)
      }
    })
  }

  fun coverBitmap(coverPath: String): Observable<Bitmap> {
    val cover = File(coverPath)
    return bitmapFromFile(cover.absolutePath)
  }

  fun coverBitmapSync(coverPath: String): Bitmap? {
    return try {
      RemoteUtils.coverBitmap(coverPath).blockingLast()
    } catch (e: Exception) {
      null
    }
  }
}