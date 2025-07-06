package com.kelsos.mbrc.features.widgets

import android.app.PendingIntent
import android.content.Context
import android.widget.RemoteViews
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.state.PlayingTrack
import com.kelsos.mbrc.platform.mediasession.RemoteIntentCode
import com.kelsos.mbrc.platform.mediasession.RemoteIntentCode.Next
import com.kelsos.mbrc.platform.mediasession.RemoteIntentCode.Play
import com.kelsos.mbrc.platform.mediasession.RemoteViewIntentBuilder.getPendingIntent
import timber.log.Timber

class WidgetNormal : WidgetBase() {
  override val type: String = "normal"
  override val config: WidgetConfig =
    WidgetConfig(
      layout = R.layout.widget_normal,
      imageSize = R.dimen.widget_normal_height,
      imageId = R.id.widget_normal_image,
      playButtonId = R.id.widget_normal_play,
      widgetClass = WidgetNormal::class
    )

  override fun setupActionIntents(
    views: RemoteViews,
    pendingIntent: PendingIntent,
    context: Context
  ) {
    Timber.v("Setting up action intents for $type widget")
    views.setOnClickPendingIntent(R.id.widget_normal_image, pendingIntent)
    views.setOnClickPendingIntent(R.id.widget_normal_play, getPendingIntent(Play, context))
    views.setOnClickPendingIntent(R.id.widget_normal_next, getPendingIntent(Next, context))
    views.setOnClickPendingIntent(
      R.id.widget_normal_previous,
      getPendingIntent(RemoteIntentCode.Previous, context)
    )
  }

  override fun setupTrackInfo(views: RemoteViews, info: PlayingTrack) {
    views.setTextViewText(R.id.widget_normal_line_one, info.title)
    views.setTextViewText(R.id.widget_normal_line_two, info.artist)
    views.setTextViewText(R.id.widget_normal_line_three, info.album)
  }
}
