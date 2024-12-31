package com.kelsos.mbrc.features.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.os.BundleCompat
import com.kelsos.mbrc.R
import com.kelsos.mbrc.annotations.PlayerState
import com.kelsos.mbrc.features.player.PlayerActivity
import com.kelsos.mbrc.features.player.TrackInfo
import com.kelsos.mbrc.platform.mediasession.RemoteViewIntentBuilder
import com.squareup.picasso.Picasso
import java.io.File

class WidgetSmall : AppWidgetProvider() {
  override fun onReceive(
    context: Context?,
    intent: Intent?,
  ) {
    super.onReceive(context, intent)
    if (intent == null || intent.action != AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
      return
    }

    val extras = intent.extras
    val widgetManager = AppWidgetManager.getInstance(context)
    if (context == null) {
      return
    }
    val widgets = ComponentName(context.packageName, WidgetSmall::class.java.name)
    val widgetsIds = widgetManager.getAppWidgetIds(widgets)

    if (extras == null) {
      return
    }

    when {
      extras.getBoolean(WidgetUpdater.COVER, false) -> {
        val path = extras.getString(WidgetUpdater.COVER_PATH, "")
        updateCover(context, widgetManager, widgetsIds, path)
      }
      extras.getBoolean(WidgetUpdater.INFO, false) -> {
        val info = BundleCompat.getParcelable<TrackInfo>(extras, WidgetUpdater.TRACK_INFO, TrackInfo::class.java)
        info?.run {
          updateInfo(context, widgetManager, widgetsIds, this)
        }
      }
      extras.getBoolean(WidgetUpdater.STATE, false) -> {
        updatePlayState(
          context,
          widgetManager,
          widgetsIds,
          extras.getString(WidgetUpdater.PLAYER_STATE, PlayerState.UNDEFINED),
        )
      }
    }
  }

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    super.onUpdate(context, appWidgetManager, appWidgetIds)

    for (appWidgetId in appWidgetIds) {
      // Create an Intent to launch ExampleActivity
      val intent = Intent(context, PlayerActivity::class.java)
      val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

      // Get the layout for the App Widget and attach an on-click listener
      // to the button
      val views = RemoteViews(context.packageName, R.layout.widget_small)

      views.setOnClickPendingIntent(R.id.widget_small_image, pendingIntent)
      views.setOnClickPendingIntent(
        R.id.widget_small_play,
        RemoteViewIntentBuilder.getPendingIntent(RemoteViewIntentBuilder.PLAY, context),
      )
      views.setOnClickPendingIntent(
        R.id.widget_small_next,
        RemoteViewIntentBuilder.getPendingIntent(RemoteViewIntentBuilder.NEXT, context),
      )
      views.setOnClickPendingIntent(
        R.id.widget_small_previous,
        RemoteViewIntentBuilder.getPendingIntent(RemoteViewIntentBuilder.PREVIOUS, context),
      )

      // Tell the AppWidgetManager to perform an update on the current app widget
      appWidgetManager.updateAppWidget(appWidgetId, views)
    }
  }

  private fun updateInfo(
    context: Context?,
    widgetManager: AppWidgetManager,
    widgetsIds: IntArray,
    info: TrackInfo,
  ) {
    if (context == null) {
      return
    }

    val smallWidget = RemoteViews(context.packageName, R.layout.widget_small)
    smallWidget.setTextViewText(R.id.widget_small_line_one, info.title)
    smallWidget.setTextViewText(R.id.widget_small_line_two, info.artist)
    widgetManager.updateAppWidget(widgetsIds, smallWidget)
  }

  private fun updateCover(
    context: Context?,
    widgetManager: AppWidgetManager,
    widgetsIds: IntArray,
    path: String,
  ) {
    if (context == null) {
      return
    }

    val smallWidget = RemoteViews(context.packageName, R.layout.widget_small)
    val coverFile = File(path)
    if (coverFile.exists()) {
      Picasso.get().invalidate(coverFile)
      Picasso
        .get()
        .load(coverFile)
        .centerCrop()
        .resizeDimen(R.dimen.widget_small_height, R.dimen.widget_small_height)
        .into(smallWidget, R.id.widget_small_image, widgetsIds)
    } else {
      smallWidget.setImageViewResource(R.id.widget_small_image, R.drawable.ic_image_no_cover)
      widgetManager.updateAppWidget(widgetsIds, smallWidget)
    }
  }

  private fun updatePlayState(
    context: Context?,
    manager: AppWidgetManager,
    widgetsIds: IntArray,
    @PlayerState.State state: String,
  ) {
    if (context == null) {
      return
    }

    val smallWidget = RemoteViews(context.packageName, R.layout.widget_small)

    smallWidget.setImageViewResource(
      R.id.widget_small_play,
      if (PlayerState.PLAYING == state) {
        R.drawable.ic_action_pause
      } else {
        R.drawable.ic_action_play
      },
    )
    manager.updateAppWidget(widgetsIds, smallWidget)
  }
}
