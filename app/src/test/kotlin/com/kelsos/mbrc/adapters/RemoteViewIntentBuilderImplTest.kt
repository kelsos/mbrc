package com.kelsos.mbrc.adapters

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.core.platform.intents.MediaIntentActions
import com.kelsos.mbrc.core.platform.mediasession.RemoteIntentCode
import com.kelsos.mbrc.ui.MainActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class RemoteViewIntentBuilderImplTest {

  private val context: Application = ApplicationProvider.getApplicationContext()
  private val builder = RemoteViewIntentBuilderImpl()

  @Test
  fun `the open intent launches the main activity`() {
    val pendingIntent = builder.getPendingIntent(RemoteIntentCode.Open, context)

    val shadow = shadowOf(pendingIntent)
    assertThat(shadow.isActivity).isTrue()
    assertThat(shadow.savedIntent.component?.className).isEqualTo(MainActivity::class.java.name)
  }

  @Test
  fun `the media intents are broadcast with their own action`() {
    val expected = mapOf(
      RemoteIntentCode.Play to MediaIntentActions.PLAY_PRESSED,
      RemoteIntentCode.Next to MediaIntentActions.NEXT_PRESSED,
      RemoteIntentCode.Close to MediaIntentActions.CLOSE_PRESSED,
      RemoteIntentCode.Previous to MediaIntentActions.PREVIOUS_PRESSED,
      RemoteIntentCode.Cancel to MediaIntentActions.CANCELLED_NOTIFICATION
    )

    expected.forEach { (code, action) ->
      val shadow = shadowOf(builder.getPendingIntent(code, context))

      assertWithMessage("%s should be a broadcast", code).that(shadow.isBroadcast).isTrue()
      assertWithMessage("action for %s", code).that(shadow.savedIntent.action).isEqualTo(action)
    }
  }

  @Test
  fun `every intent code keeps its own request code`() {
    val codes = listOf(
      RemoteIntentCode.Open,
      RemoteIntentCode.Play,
      RemoteIntentCode.Next,
      RemoteIntentCode.Close,
      RemoteIntentCode.Previous,
      RemoteIntentCode.Cancel
    )

    codes.forEach { code ->
      val shadow = shadowOf(builder.getPendingIntent(code, context))

      assertWithMessage("request code for %s", code)
        .that(shadow.requestCode)
        .isEqualTo(code.code)
    }
  }

  @Test
  fun `pending intents are immutable and update the existing one`() {
    val shadow = shadowOf(builder.getPendingIntent(RemoteIntentCode.Play, context))

    assertThat(shadow.isImmutable).isTrue()
    assertThat(shadow.flags and PendingIntent.FLAG_UPDATE_CURRENT).isNotEqualTo(0)
  }

  @Test
  fun `the launch pending intent opens the main activity with the open request code`() {
    val shadow = shadowOf(builder.getLaunchPendingIntent(context))

    assertThat(shadow.isActivity).isTrue()
    assertThat(shadow.requestCode).isEqualTo(RemoteIntentCode.Open.code)
    assertThat(shadow.savedIntent.component?.className).isEqualTo(MainActivity::class.java.name)
  }

  @Test
  fun `the launch intent reuses an existing task rather than stacking activities`() {
    val intent = builder.getLaunchIntent(context)

    assertThat(intent.component?.className).isEqualTo(MainActivity::class.java.name)
    assertThat(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP).isNotEqualTo(0)
  }
}
