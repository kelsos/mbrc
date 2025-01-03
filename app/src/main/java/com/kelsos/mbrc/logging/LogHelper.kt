package com.kelsos.mbrc.logging

import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.logging.FileLoggingTree.Companion.LOGS_DIR
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

interface LogHelper {
  suspend fun logsExist(filesDir: File): Boolean

  suspend fun zipLogs(
    filesDir: File,
    cacheDir: File?,
  ): File
}

class LogHelperImpl(
  private val appCoroutineDispatchers: AppCoroutineDispatchers,
) : LogHelper {
  override suspend fun logsExist(filesDir: File): Boolean =
    withContext(appCoroutineDispatchers.io) {
      try {
        val logDir = File(filesDir, LOGS_DIR)
        logDir.listFiles()?.any { it.extension != "lck" } == true
      } catch (e: SecurityException) {
        Timber.e(e, "Log access failed")
        return@withContext false
      }
    }

  override suspend fun zipLogs(
    filesDir: File,
    cacheDir: File?,
  ): File =
    withContext(appCoroutineDispatchers.io) {
      val logDir = File(filesDir, LOGS_DIR)
      if (!logDir.exists()) {
        throw FileNotFoundException(logDir.canonicalPath)
      }

      val logFiles =
        logDir.listFiles()?.filter {
          it.extension != "lck"
        }

      if (logFiles.isNullOrEmpty()) {
        throw FileNotFoundException("No log files found")
      }

      val buffer = ByteArray(size = 1024)
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

      zipFile
    }

  companion object {
    private const val LOG_ZIP = "mbrc_logs.zip"
  }
}
