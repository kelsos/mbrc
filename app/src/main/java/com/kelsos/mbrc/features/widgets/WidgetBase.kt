package com.kelsos.mbrc.features.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import com.kelsos.mbrc.R
import com.kelsos.mbrc.annotations.PlayerState
import com.kelsos.mbrc.common.utilities.whenNotNull
import com.kelsos.mbrc.features.player.PlayerActivity
import com.kelsos.mbrc.features.player.TrackInfo
import timber.log.Timber
import java.io.File

abstract class WidgetBase : AppWidgetProvider() {
  abstract val config: WidgetConfig
  abstract val type: String

  override fun onReceive(
    context: Context?,
    intent: Intent?,
  ) {
    super.onReceive(context, intent)
    if (intent?.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
      whenNotNull(context, intent.extras, this::updateWidget)
    }
  }

  private fun updateWidget(
    context: Context,
    extras: Bundle,
  ) {
    val widgetManager = AppWidgetManager.getInstance(context)
    val widgets = ComponentName(context.packageName, config.widgetClass.java.name)
    val widgetsIds = widgetManager.getAppWidgetIds(widgets)
    val data = BundleData(extras)

    if (widgetsIds.isEmpty()) {
      Timber.v("No $type widgets found for update")
      return
    }

    Timber.v("Updating $type widgets ${widgetsIds.joinToString(", ")} with extras: $data")

    when {
      data.isCover() -> {
        updateCover(context, widgetManager, widgetsIds, data.cover())
      }
      data.isInfo() ->
        updateInfo(
          context,
          widgetManager,
          widgetsIds,
          data.playingTrack(),
        )
      data.isState() ->
        updatePlayState(
          context,
          widgetManager,
          widgetsIds,
          data.state(),
        )
    }
  }

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    super.onUpdate(context, appWidgetManager, appWidgetIds)

    if (!appWidgetIds.isEmpty()) {
      Timber.v("onUpdate called for $type widgets: ${appWidgetIds.joinToString(", ")}")
    }

    for (appWidgetId in appWidgetIds) {
      val intent = Intent(context, PlayerActivity::class.java)
      val pendingIntent =
        PendingIntent.getActivity(
          context,
          0,
          intent,
          PendingIntent.FLAG_IMMUTABLE,
        )
      val views = RemoteViews(context.packageName, config.layout)
      setupActionIntents(views, pendingIntent, context)
      // Tell the AppWidgetManager to perform an set on the current app widget
      appWidgetManager.updateAppWidget(appWidgetId, views)
    }
  }

  abstract fun setupActionIntents(
    views: RemoteViews,
    pendingIntent: PendingIntent,
    context: Context,
  )

  abstract fun setupTrackInfo(
    views: RemoteViews,
    info: TrackInfo,
  )

  private fun updateInfo(
    context: Context,
    widgetManager: AppWidgetManager,
    widgetsIds: IntArray,
    info: TrackInfo,
  ) {
    val views = RemoteViews(context.packageName, config.layout)
    setupTrackInfo(views, info)
    widgetManager.updateAppWidget(widgetsIds, views)
  }

  private fun updateCover(
    context: Context,
    widgetManager: AppWidgetManager,
    widgetsIds: IntArray,
    path: String,
  ) {
    val widget = RemoteViews(context.packageName, config.layout)
    val coverFile = File(path)
    if (coverFile.exists()) {
      val request =
        ImageRequest
          .Builder(context)
          .data(coverFile)
          .size(R.dimen.widget_small_height)
          .scale(Scale.FILL)
          .precision(Precision.INEXACT)
          .target(RemoteViewsTarget(widgetManager, widget, widgetsIds, config.imageId))
          .build()

      context.imageLoader.enqueue(request)
    } else {
      widget.setImageViewResource(config.imageId, R.drawable.ic_image_no_cover)
      widgetManager.updateAppWidget(widgetsIds, widget)
    }
  }

  private fun updatePlayState(
    context: Context,
    manager: AppWidgetManager,
    widgetsIds: IntArray,
    state: String,
  ) {
    val widget = RemoteViews(context.packageName, config.layout)

    widget.setImageViewResource(
      config.playButtonId,
      if (PlayerState.PLAYING == state) {
        R.drawable.ic_action_pause
      } else {
        R.drawable.ic_action_play
      },
    )
    manager.updateAppWidget(widgetsIds, widget)
  }
}
