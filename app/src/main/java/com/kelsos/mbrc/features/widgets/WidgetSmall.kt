package com.kelsos.mbrc.features.widgets

import android.app.PendingIntent
import android.content.Context
import android.widget.RemoteViews
import com.kelsos.mbrc.R
import com.kelsos.mbrc.features.player.TrackInfo
import com.kelsos.mbrc.platform.mediasession.RemoteIntentCode.Next
import com.kelsos.mbrc.platform.mediasession.RemoteIntentCode.Play
import com.kelsos.mbrc.platform.mediasession.RemoteIntentCode.Previous
import com.kelsos.mbrc.platform.mediasession.RemoteViewIntentBuilder.getPendingIntent

class WidgetSmall : WidgetBase() {
  override val type: String = "small"
  override val config: WidgetConfig =
    WidgetConfig(
      layout = R.layout.widget_small,
      imageSize = R.dimen.widget_small_height,
      imageId = R.id.widget_small_image,
      playButtonId = R.id.widget_small_play,
      widgetClass = WidgetSmall::class,
    )

  override fun setupActionIntents(
    views: RemoteViews,
    pendingIntent: PendingIntent,
    context: Context,
  ) {
    views.setOnClickPendingIntent(R.id.widget_small_image, pendingIntent)
    views.setOnClickPendingIntent(R.id.widget_small_play, getPendingIntent(Play, context))
    views.setOnClickPendingIntent(R.id.widget_small_next, getPendingIntent(Next, context))
    views.setOnClickPendingIntent(R.id.widget_small_previous, getPendingIntent(Previous, context))
  }

  override fun setupTrackInfo(
    views: RemoteViews,
    info: TrackInfo,
  ) {
    views.setTextViewText(R.id.widget_small_line_one, info.title)
    views.setTextViewText(R.id.widget_small_line_two, info.artist)
  }
}
