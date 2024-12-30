package com.kelsos.mbrc.logging

import android.content.Context
import com.kelsos.mbrc.logging.FileLoggingTree.Companion.LOGS_DIR
import rx.Single
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object LogHelper {
  fun logsExist(context: Context): Single<Boolean> {
    return Single.fromCallable {
      val filesDir = context.filesDir
      val logDir = File(filesDir, FileLoggingTree.LOGS_DIR)
      val logFiles = logDir.listFiles().filter { it.extension != "lck" }
      return@fromCallable logFiles.isNotEmpty()
    }
  }

  fun zipLogs(context: Context): Single<File> {
    return Single.fromCallable {
      val filesDir = context.filesDir
      val cacheDir = context.externalCacheDir
      val logDir = File(filesDir, FileLoggingTree.LOGS_DIR)
      if (!logDir.exists()) {
        throw FileNotFoundException(logDir.canonicalPath)
      }

      val logFiles =
        logDir.listFiles().filter {
          it.extension != "lck"
        }

      if (logFiles.isEmpty()) {
        throw RuntimeException("No log files found")
      }

      try {
        val buffer = ByteArray(1024)

        val zipDir = File(cacheDir, LOGS_DIR)
        if (!zipDir.exists()) {
          zipDir.mkdir()
        }
        val zipFile = File(zipDir, LOG_ZIP)

        if (zipFile.exists()) {
          zipFile.delete()
        }

        val fos = FileOutputStream(zipFile)
        val zos = ZipOutputStream(fos)
        logFiles.forEach {
          val ze = ZipEntry(it.name)
          zos.putNextEntry(ze)
          val fin = FileInputStream(it)

          var len: Int
          do {
            len = fin.read(buffer)
            if (len <= 0) {
              break
            }
            zos.write(buffer, 0, len)
          } while (true)

          fin.close()
          zos.closeEntry()
        }

        zos.close()
        fos.flush()
        fos.close()

        return@fromCallable zipFile
      } catch (e: IOException) {
        throw RuntimeException(e)
      }
    }
  }

  const val LOG_ZIP = "mbrc_logs.zip"
}
