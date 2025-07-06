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
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.error
import coil3.size.Precision
import coil3.size.Scale
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.state.PlayerState
import com.kelsos.mbrc.common.state.PlayingTrack
import com.kelsos.mbrc.common.utilities.whenNotNull
import com.kelsos.mbrc.features.player.PlayerActivity
import timber.log.Timber

abstract class WidgetBase : AppWidgetProvider() {
  abstract val config: WidgetConfig
  abstract val type: String

  override fun onReceive(context: Context?, intent: Intent?) {
    super.onReceive(context, intent)
    when (intent?.action) {
      AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
        whenNotNull(context, intent.extras, this::updateWidget)
      }
      else -> {
        val extras = intent?.extras
        if (context == null || extras == null) {
          return
        }

        val data = BundleData(extras)
        if (data.isInfo() || data.isState()) {
          updateWidget(context, extras)
        }
      }
    }
  }

  private fun updateWidget(context: Context, extras: Bundle) {
    try {
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
        data.isInfo() -> {
          val playingTrack = data.playingTrack()
          updateInfoWithCover(
            context,
            widgetManager,
            widgetsIds,
            playingTrack
          )
        }
        data.isState() ->
          updatePlayState(
            context,
            widgetManager,
            widgetsIds,
            data.state()
          )
      }
    } catch (e: SecurityException) {
      Timber.e(e, "Security error updating $type widget - check permissions")
    } catch (e: IllegalArgumentException) {
      Timber.e(e, "Invalid arguments for $type widget update")
    }
  }

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
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
          PendingIntent.FLAG_IMMUTABLE
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
    context: Context
  )

  abstract fun setupTrackInfo(views: RemoteViews, info: PlayingTrack)

  private fun updateInfoWithCover(
    context: Context,
    widgetManager: AppWidgetManager,
    widgetsIds: IntArray,
    info: PlayingTrack
  ) {
    Timber.d("updateInfoWithCover called for $type widget, coverUrl: '${info.coverUrl}'")

    val widget = RemoteViews(context.packageName, config.layout)
    setupTrackInfo(widget, info)
    preserveActions(context, widget)

    if (info.coverUrl.isNotBlank()) {
      // Load image asynchronously, but the widget is already set up with track info
      val target =
        RemoteViewsTarget(
          widgetManager,
          widget,
          widgetsIds,
          config.imageId,
          onImageUpdated = {
            // Callback to preserve actions after the image loads
            preserveActions(context, widget)
          }
        )

      val request =
        ImageRequest
          .Builder(context)
          .data(info.coverUrl)
          .size(R.dimen.widget_small_height)
          .scale(Scale.FILL)
          .error(R.drawable.ic_image_no_cover)
          .precision(Precision.INEXACT)
          .memoryCachePolicy(CachePolicy.DISABLED)
          .diskCachePolicy(CachePolicy.ENABLED)
          .target(target)
          .build()

      context.imageLoader.enqueue(request)
    } else {
      // No cover URL, use placeholder and update immediately
      widget.setImageViewResource(config.imageId, R.drawable.ic_image_no_cover)
      widgetManager.updateAppWidget(widgetsIds, widget)
    }
  }

  private fun preserveActions(context: Context, views: RemoteViews) {
    val intent = Intent(context, PlayerActivity::class.java)
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE
      )
    setupActionIntents(views, pendingIntent, context)
  }

  private fun updatePlayState(
    context: Context,
    manager: AppWidgetManager,
    widgetsIds: IntArray,
    state: String
  ) {
    val widget = RemoteViews(context.packageName, config.layout)

    widget.setImageViewResource(
      config.playButtonId,
      if (PlayerState.PLAYING == state) {
        R.drawable.baseline_pause_24
      } else {
        R.drawable.baseline_play_arrow_24
      }
    )

    preserveActions(context, widget)

    manager.updateAppWidget(widgetsIds, widget)
  }
}
