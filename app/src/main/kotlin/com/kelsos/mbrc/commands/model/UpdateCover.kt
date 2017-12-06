package com.kelsos.mbrc.commands.model

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.JPEG
import android.graphics.BitmapFactory
import android.util.Base64
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.CoverPayload
import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.CoverChangedEvent
import com.kelsos.mbrc.events.ui.RemoteClientMetaData
import com.kelsos.mbrc.extensions.getDimens
import com.kelsos.mbrc.extensions.md5
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.MainDataModel
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.repository.ModelCache
import com.kelsos.mbrc.widgets.UpdateWidgets
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

class UpdateCover
@Inject
constructor(
  private val bus: RxBus,
  private val context: Application,
  private val mapper: ObjectMapper,
  private val coverService: ApiBase,
  private val model: MainDataModel,
  private val cache: ModelCache,
  appDispatchers: AppDispatchers
) : ICommand {
  private val coverDir: File = File(context.filesDir, COVER_DIR)
  private val job = SupervisorJob()
  private val scope = CoroutineScope(job + appDispatchers.io)

  override fun execute(e: IEvent) {
    val payload = mapper.treeToValue((e.data as JsonNode), CoverPayload::class.java)

    if (payload.status == CoverPayload.NOT_FOUND) {
      bus.post(CoverChangedEvent())
      UpdateWidgets.updateCover(context)
    } else if (payload.status == CoverPayload.READY) {
      scope.launch {
        retrieveCover()
      }
    }
  }

  private suspend fun retrieveCover() {
    val (status, cover) = try {
      coverService.getItem(Protocol.NowPlayingCover, CoverPayload::class)
    } catch (e: Exception) {
      return
    }

    if (status != CoverPayload.SUCCESS) {
      removeCover()
      return
    }

    try {
      val bitmap = getBitmap(cover)
      val file = storeCover(bitmap)
      val pre = prefetch(file)
      val path = pre.absolutePath
      model.coverPath = path
      savePath(path)
      bus.post(CoverChangedEvent(path))
      bus.post(RemoteClientMetaData(model.trackInfo, model.coverPath, model.duration))
      UpdateWidgets.updateCover(context, path)
    } catch (e: Exception) {
      removeCover(e)
    }

    Timber.v("Message received for available cover")
    return
  }

  private fun savePath(path: String) {
    scope.launch {
      try {
        cache.persistCover(path)
        Timber.v("Playing track cover $path successfully persisted")
      } catch (e: Exception) {
        Timber.v(e, "Failed to persist the playing track cover")
      }
    }
  }

  private fun getBitmap(base64: String): Bitmap {
    val decodedImage = Base64.decode(base64, Base64.DEFAULT)
    val bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size)
    if (bitmap != null) {
      return bitmap
    } else {
      throw RuntimeException("Base64 was not an image")
    }
  }

  private fun removeCover(it: Throwable? = null) {
    clearPreviousCovers(0)

    it?.let {
      Timber.v(it, "Failed to store path")
    }

    bus.post(CoverChangedEvent())
  }

  private fun storeCover(bitmap: Bitmap): File {
    checkIfExists()
    clearPreviousCovers()
    val file = temporaryCover()
    val fileStream = FileOutputStream(file)
    val success = bitmap.compress(JPEG, 100, fileStream)
    fileStream.close()
    if (success) {
      val md5 = file.md5()
      val extension = file.extension
      val newFile = File(coverDir, "$md5.$extension")
      file.renameTo(newFile)
      Timber.v("file was renamed to %s", newFile.absolutePath)
      return newFile
    } else {
      throw RuntimeException("unable to store cover")
    }
  }

  private suspend fun prefetch(newFile: File): File = suspendCancellableCoroutine { cont ->
    val dimens = context.getDimens()
    Picasso.get().load(newFile)
      .config(Bitmap.Config.RGB_565)
      .resize(dimens, dimens)
      .centerCrop()
      .fetch(object : Callback {
        override fun onSuccess() {
          cont.resume(newFile) { cause, _, _ -> }
        }

        override fun onError(e: Exception) {
          cont.resumeWithException(e)
        }
      })
  }

  private fun checkIfExists() {
    if (!coverDir.exists()) {
      coverDir.mkdir()
    }
  }

  private fun clearPreviousCovers(keep: Int = 2) {
    coverDir.listFiles()?.run {
      sortByDescending(File::lastModified)
      Timber.v(size.toString())
      val toDelete = if (size - keep < 0) 0 else size - keep
      Timber.v("deleting last $toDelete items")
      takeLast(toDelete).forEach {
        it.delete()
      }
    }
  }

  private fun temporaryCover(): File {
    val file = File(context.cacheDir, TEMP_COVER)
    if (file.exists()) {
      file.delete()
    }
    return file
  }

  companion object {
    const val COVER_DIR = "cover"
    const val TEMP_COVER = "temp_cover.jpg"
  }

}

