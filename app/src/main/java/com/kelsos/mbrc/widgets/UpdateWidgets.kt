package com.kelsos.mbrc.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import com.kelsos.mbrc.annotations.PlayerState.State
import com.kelsos.mbrc.domain.TrackInfo

object UpdateWidgets {
  const val COVER = "com.kelsos.mbrc.widgets.COVER"
  const val COVER_PATH = "com.kelsos.mbrc.widgets.COVER_PATH"
  const val STATE = "com.kelsos.mbrc.widgets.STATE"
  const val INFO = "com.kelsos.mbrc.widgets.INFO"
  const val TRACK_INFO = "com.kelsos.mbrc.widgets.TRACKINFO"
  const val PLAYER_STATE = "com.kelsos.mbrc.widgets.PLAYER_STATE"

  fun updateCover(context: Context, path: String = "") {
    val normalIntent = getIntent(WidgetNormal::class.java, context)
      .putExtra(COVER, true)
      .putExtra(COVER_PATH, path)
    val smallIntent = getIntent(WidgetSmall::class.java, context)
      .putExtra(COVER, true)
      .putExtra(COVER_PATH, path)

    context.sendBroadcast(smallIntent)
    context.sendBroadcast(normalIntent)
  }

  fun updatePlaystate(context: Context, @State state: String) {
    val normalIntent = getIntent(WidgetNormal::class.java, context)
      .putExtra(STATE, true)
      .putExtra(PLAYER_STATE, state)

    val smallIntent = getIntent(WidgetSmall::class.java, context)
      .putExtra(STATE, true)
      .putExtra(PLAYER_STATE, state)

    context.sendBroadcast(smallIntent)
    context.sendBroadcast(normalIntent)
  }

  fun updateTrackInfo(context: Context, info: TrackInfo) {
    val normalIntent = getIntent(WidgetNormal::class.java, context)
      .putExtra(INFO, true)
      .putExtra(TRACK_INFO, info)

    val smallIntent = getIntent(WidgetSmall::class.java, context)
      .putExtra(INFO, true)
      .putExtra(TRACK_INFO, info)

    context.sendBroadcast(smallIntent)
    context.sendBroadcast(normalIntent)
  }

  private fun getIntent(clazz: Class<*>, context: Context): Intent {
    val widgetUpdateIntent = Intent(context, clazz)
    widgetUpdateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    return widgetUpdateIntent
  }


}
