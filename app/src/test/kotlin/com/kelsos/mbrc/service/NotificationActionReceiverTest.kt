package com.kelsos.mbrc.service

import android.Manifest
import android.app.Application
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.core.networking.protocol.actions.UserAction
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import com.kelsos.mbrc.core.networking.protocol.usecases.UserActionUseCase
import com.kelsos.mbrc.core.networking.protocol.usecases.VolumeModifyUseCase
import com.kelsos.mbrc.core.platform.intents.MediaIntentActions
import com.kelsos.mbrc.feature.settings.data.CallAction
import com.kelsos.mbrc.feature.settings.domain.SettingsManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * The receiver dispatches on the main dispatcher, so [Dispatchers.setMain] is installed with an
 * unconfined dispatcher before the receiver is constructed: it captures the dispatcher in a field
 * at construction time, and the ringing path has to complete before the assertions run.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class NotificationActionReceiverTest {

  private val context: Application = ApplicationProvider.getApplicationContext()
  private val callAction = MutableStateFlow<CallAction>(CallAction.None)
  private val settingsManager = mockk<SettingsManager>()
  private val userActionUseCase = mockk<UserActionUseCase>(relaxed = true)
  private val volumeModifyUseCase = mockk<VolumeModifyUseCase>(relaxed = true)

  private lateinit var receiver: NotificationActionReceiver

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    every { settingsManager.incomingCallActionFlow } returns callAction
    receiver = NotificationActionReceiver(settingsManager, userActionUseCase, volumeModifyUseCase)
  }

  @After
  fun tearDown() {
    ServiceState.setStopping(false)
    Dispatchers.resetMain()
  }

  @Test
  fun `filter listens for phone state when permitted and a call action is set`() {
    shadowOf(context).grantPermissions(Manifest.permission.READ_PHONE_STATE)
    callAction.value = CallAction.Pause

    val filter = receiver.filter(context)

    assertThat(filter.hasAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)).isTrue()
  }

  @Test
  fun `filter ignores phone state without the read phone state permission`() {
    shadowOf(context).denyPermissions(Manifest.permission.READ_PHONE_STATE)
    callAction.value = CallAction.Pause

    val filter = receiver.filter(context)

    assertThat(filter.hasAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)).isFalse()
  }

  @Test
  fun `filter ignores phone state when no call action is set`() {
    shadowOf(context).grantPermissions(Manifest.permission.READ_PHONE_STATE)
    callAction.value = CallAction.None

    val filter = receiver.filter(context)

    assertThat(filter.hasAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)).isFalse()
  }

  @Test
  fun `filter always listens for the media button actions`() {
    val filter = receiver.filter(context)

    val actions = listOf(
      MediaIntentActions.PLAY_PRESSED,
      MediaIntentActions.NEXT_PRESSED,
      MediaIntentActions.CLOSE_PRESSED,
      MediaIntentActions.PREVIOUS_PRESSED,
      MediaIntentActions.CANCELLED_NOTIFICATION
    )
    actions.forEach { action ->
      assertWithMessage("filter should include %s", action)
        .that(filter.hasAction(action))
        .isTrue()
    }
  }

  @Test
  fun `play press performs a play pause action`() {
    receiver.onReceive(context, Intent(MediaIntentActions.PLAY_PRESSED))

    verify { userActionUseCase.tryPerform(UserAction.create(Protocol.PlayerPlayPause)) }
  }

  @Test
  fun `next press performs a next action`() {
    receiver.onReceive(context, Intent(MediaIntentActions.NEXT_PRESSED))

    verify { userActionUseCase.tryPerform(UserAction.create(Protocol.PlayerNext)) }
  }

  @Test
  fun `previous press performs a previous action`() {
    receiver.onReceive(context, Intent(MediaIntentActions.PREVIOUS_PRESSED))

    verify { userActionUseCase.tryPerform(UserAction.create(Protocol.PlayerPrevious)) }
  }

  @Test
  fun `close press stops the remote service`() {
    receiver.onReceive(context, Intent(MediaIntentActions.CLOSE_PRESSED))

    val stopped = shadowOf(context).nextStoppedService
    assertThat(stopped.component?.className).isEqualTo(RemoteService::class.java.name)
  }

  @Test
  fun `a cancelled notification stops the remote service`() {
    receiver.onReceive(context, Intent(MediaIntentActions.CANCELLED_NOTIFICATION))

    val stopped = shadowOf(context).nextStoppedService
    assertThat(stopped.component?.className).isEqualTo(RemoteService::class.java.name)
  }

  @Test
  fun `a service already stopping is not stopped again`() {
    ServiceState.setStopping(true)

    receiver.onReceive(context, Intent(MediaIntentActions.CLOSE_PRESSED))

    assertThat(shadowOf(context).nextStoppedService).isNull()
  }

  @Test
  fun `an unknown action does nothing`() {
    receiver.onReceive(context, Intent("com.kelsos.mbrc.notification.unknown"))

    verify(exactly = 0) { userActionUseCase.tryPerform(any()) }
    assertThat(shadowOf(context).nextStoppedService).isNull()
  }

  @Test
  fun `a ringing call pauses playback when the pause action is set`() {
    callAction.value = CallAction.Pause

    receiver.onReceive(context, ringing())

    verify { userActionUseCase.tryPerform(UserAction.create(Protocol.PlayerPause)) }
  }

  @Test
  fun `a ringing call stops playback when the stop action is set`() {
    callAction.value = CallAction.Stop

    receiver.onReceive(context, ringing())

    verify { userActionUseCase.tryPerform(UserAction.create(Protocol.PlayerStop)) }
  }

  @Test
  fun `a ringing call reduces the volume when the reduce action is set`() {
    callAction.value = CallAction.Reduce

    receiver.onReceive(context, ringing())

    coVerify { volumeModifyUseCase.reduceVolume() }
  }

  @Test
  fun `a ringing call is ignored when no call action is set`() {
    callAction.value = CallAction.None

    receiver.onReceive(context, ringing())

    verify(exactly = 0) { userActionUseCase.tryPerform(any()) }
    coVerify(exactly = 0) { volumeModifyUseCase.reduceVolume() }
  }

  @Test
  fun `a call state other than ringing is ignored`() {
    callAction.value = CallAction.Pause

    receiver.onReceive(context, phoneState(TelephonyManager.EXTRA_STATE_IDLE))

    verify(exactly = 0) { userActionUseCase.tryPerform(any()) }
  }

  @Test
  fun `a phone state broadcast without extras is ignored`() {
    callAction.value = CallAction.Pause

    receiver.onReceive(context, Intent(TelephonyManager.ACTION_PHONE_STATE_CHANGED))

    verify(exactly = 0) { userActionUseCase.tryPerform(any()) }
  }

  private fun ringing(): Intent = phoneState(TelephonyManager.EXTRA_STATE_RINGING)

  private fun phoneState(state: String): Intent =
    Intent(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
      .putExtra(TelephonyManager.EXTRA_STATE, state)
}
