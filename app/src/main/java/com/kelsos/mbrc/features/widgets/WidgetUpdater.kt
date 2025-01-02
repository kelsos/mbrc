package com.kelsos.mbrc.features.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import com.kelsos.mbrc.features.player.TrackInfo

interface WidgetUpdater {
  fun updatePlayingTrack(track: TrackInfo)

  fun updatePlayState(state: String)

  fun updateCover(path: String = "")

  companion object {
    const val COVER = "com.kelsos.mbrc.features.widgets.COVER"
    const val COVER_PATH = "com.kelsos.mbrc.features.widgets.COVER_PATH"
    const val STATE = "com.kelsos.mbrc.features.widgets.STATE"
    const val INFO = "com.kelsos.mbrc.features.widgets.INFO"
    const val TRACK_INFO = "com.kelsos.mbrc.features.widgets.TRACKINFO"
    const val PLAYER_STATE = "com.kelsos.mbrc.features.widgets.PLAYER_STATE"
  }
}

class WidgetUpdaterImpl(
  private val context: Context,
) : WidgetUpdater {
  private fun createIntent(clazz: Class<*>): Intent {
    val widgetUpdateIntent = Intent(context, clazz)
    widgetUpdateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    return widgetUpdateIntent
  }

  private fun Intent.payload(track: TrackInfo): Intent =
    putExtra(WidgetUpdater.INFO, true)
      .putExtra(WidgetUpdater.TRACK_INFO, track)

  private fun Intent.payload(path: String): Intent =
    putExtra(WidgetUpdater.COVER, true)
      .putExtra(WidgetUpdater.COVER_PATH, path)

  private fun Intent.statePayload(state: String): Intent =
    putExtra(WidgetUpdater.STATE, true)
      .putExtra(WidgetUpdater.PLAYER_STATE, state)

  override fun updatePlayingTrack(track: TrackInfo) {
    val normalIntent = createIntent(WidgetNormal::class.java).payload(track)
    val smallIntent = createIntent(WidgetSmall::class.java).payload(track)
    broadcast(smallIntent, normalIntent)
  }

  override fun updatePlayState(state: String) {
    val normalIntent = createIntent(WidgetNormal::class.java).statePayload(state)
    val smallIntent = createIntent(WidgetSmall::class.java).statePayload(state)
    broadcast(smallIntent, normalIntent)
  }

  override fun updateCover(path: String) {
    val normalIntent = createIntent(WidgetNormal::class.java).payload(path)
    val smallIntent = createIntent(WidgetSmall::class.java).payload(path)
    broadcast(smallIntent, normalIntent)
  }

  private fun broadcast(
    smallIntent: Intent,
    normalIntent: Intent,
  ) = with(context) {
    sendBroadcast(smallIntent)
    sendBroadcast(normalIntent)
  }
}
