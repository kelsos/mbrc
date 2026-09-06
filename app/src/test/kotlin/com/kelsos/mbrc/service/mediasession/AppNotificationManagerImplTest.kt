package com.kelsos.mbrc.service.mediasession

import android.app.Application
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.core.common.state.AppState
import com.kelsos.mbrc.core.common.state.BasicTrackInfo
import com.kelsos.mbrc.core.common.state.PlayerState
import com.kelsos.mbrc.core.common.state.PlayingPosition
import com.kelsos.mbrc.core.common.test.testDispatchers
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.networking.protocol.usecases.UserActionUseCase
import com.kelsos.mbrc.core.networking.protocol.usecases.VolumeModifyUseCase
import com.kelsos.mbrc.core.platform.mediasession.NotificationData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The track and state updates are posted from [MediaSessionManager.scope], which runs on a real
 * background dispatcher, so those assertions wait on [built] rather than on a test scheduler. The
 * session manager itself keeps the standard test dispatchers: its collectors must stay parked, or
 * they would touch Media3 off the main thread.
 *
 * No `coroutineTestTimeout()` rule here: it would move the test off the SDK main thread, and Media3
 * rejects session creation from anywhere else.
 */
@RunWith(AndroidJUnit4::class)
class AppNotificationManagerImplTest {

  private val context: Application = ApplicationProvider.getApplicationContext()
  private val notificationManager = mockk<NotificationManager>(relaxed = true)
  private val notificationBuilder = mockk<NotificationBuilder>()
  private val built = LinkedBlockingQueue<NotificationData>()
  private val placeholders = LinkedBlockingQueue<Unit>()

  private val immediateDispatchers = object : AppCoroutineDispatchers {
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
    override val database: CoroutineDispatcher = Dispatchers.Unconfined
    override val network: CoroutineDispatcher = Dispatchers.Unconfined
  }

  private lateinit var sessionManager: MediaSessionManager
  private lateinit var manager: AppNotificationManagerImpl

  @Before
  fun setUp() {
    every { notificationBuilder.createBuilder(any(), any()) } answers {
      built.put(firstArg())
      NotificationCompat.Builder(context, AppNotificationManager.CHANNEL_ID)
    }
    every { notificationBuilder.createPlaceholderBuilder() } answers {
      placeholders.put(Unit)
      NotificationCompat.Builder(context, AppNotificationManager.CHANNEL_ID)
    }

    sessionManager = MediaSessionManager(
      context = context,
      userActionUseCase = mockk<UserActionUseCase>(relaxed = true),
      volumeModifyUseCase = mockk<VolumeModifyUseCase>(relaxed = true),
      appState = AppState(),
      dispatchers = testDispatchers
    )
    manager = AppNotificationManagerImpl(
      context = context,
      dispatchers = immediateDispatchers,
      notificationManager = notificationManager,
      notificationBuilder = notificationBuilder,
      mediaSessionManager = sessionManager
    )
  }

  @After
  fun tearDown() {
    sessionManager.destroy()
  }

  @Test
  fun `the session channel exists once the manager is constructed`() {
    val channel = NotificationManagerCompat
      .from(context)
      .getNotificationChannel(AppNotificationManager.CHANNEL_ID)

    assertThat(channel).isNotNull()
  }

  @Test
  fun `initialize starts the media session`() {
    manager.initialize()

    assertThat(sessionManager.mediaSession).isNotNull()
  }

  @Test
  fun `destroy tears the media session down`() {
    manager.initialize()

    manager.destroy()

    assertThat(sessionManager.mediaSession).isNull()
  }

  @Test
  fun `cancel removes the media notification by default`() {
    manager.cancel()

    verify { notificationManager.cancel(AppNotificationManager.MEDIA_SESSION_NOTIFICATION_ID) }
  }

  @Test
  fun `cancel removes the requested notification`() {
    manager.cancel(42)

    verify { notificationManager.cancel(42) }
  }

  @Test
  fun `connecting posts a notification`() {
    manager.initialize()

    manager.connectionStateChanged(connected = true)

    assertThat(built).hasSize(1)
    verify { notificationManager.notify(MEDIA_NOTIFICATION_ID, any()) }
  }

  @Test
  fun `connecting without a session posts nothing`() {
    manager.connectionStateChanged(connected = true)

    assertWithMessage("there is no session to attach the notification to")
      .that(built)
      .isEmpty()
    verify(exactly = 0) { notificationManager.notify(any(), any()) }
  }

  @Test
  fun `disconnecting replaces the notification with a placeholder`() {
    manager.initialize()

    manager.connectionStateChanged(connected = false)

    assertThat(placeholders).hasSize(1)
    verify { notificationManager.notify(MEDIA_NOTIFICATION_ID, any()) }
  }

  @Test
  fun `disconnecting drops the track that was showing`() {
    manager.initialize()
    manager.updatePlayingTrack(track(title = "Tarkus"))
    awaitBuilt()

    manager.connectionStateChanged(connected = false)
    manager.connectionStateChanged(connected = true)

    assertWithMessage("stale track data survived the disconnect")
      .that(awaitBuilt().track.title)
      .isEmpty()
  }

  @Test
  fun `a playing track is carried into the notification`() {
    manager.initialize()

    manager.updatePlayingTrack(track(title = "Tarkus", artist = "Emerson, Lake & Palmer"))

    val data = awaitBuilt()
    assertThat(data.track.title).isEqualTo("Tarkus")
    assertThat(data.track.artist).isEqualTo("Emerson, Lake & Palmer")
  }

  @Test
  fun `a track without a cover url is posted without a bitmap`() {
    manager.initialize()

    manager.updatePlayingTrack(track(title = "Tarkus"))

    assertThat(awaitBuilt().cover).isNull()
  }

  /**
   * Robolectric's `BitmapFactory` hands back a bitmap for any path, so this pins the decode
   * happening at all, not the on-device failure path where a missing file decodes to null.
   */
  @Test
  fun `a cover url is decoded into the notification`() {
    manager.initialize()

    manager.updatePlayingTrack(track(title = "Tarkus", coverUrl = "file:///covers/tarkus.jpg"))

    val data = awaitBuilt()
    assertThat(data.cover).isNotNull()
    assertThat(data.track.title).isEqualTo("Tarkus")
  }

  @Test
  fun `the player state is carried into the notification`() {
    manager.initialize()

    manager.updateState(PlayerState.Playing, PlayingPosition(current = 1_000, total = 200_000))

    val data = awaitBuilt()
    assertThat(data.playerState).isEqualTo(PlayerState.Playing)
    assertThat(data.isStream).isFalse()
    assertWithMessage("a track with a known duration needs no elapsed time in the notification")
      .that(data.elapsedTime)
      .isEmpty()
  }

  @Test
  fun `a stream shows the elapsed time instead of a duration`() {
    manager.initialize()

    manager.updateState(PlayerState.Playing, PlayingPosition(current = 65_000, total = -1))

    val data = awaitBuilt()
    assertThat(data.isStream).isTrue()
    assertThat(data.elapsedTime).isEqualTo("01:05")
  }

  @Test
  fun `a state update keeps the track that was already showing`() {
    manager.initialize()
    manager.updatePlayingTrack(track(title = "Tarkus"))
    awaitBuilt()

    manager.updateState(PlayerState.Paused, PlayingPosition())

    assertThat(awaitBuilt().track.title).isEqualTo("Tarkus")
  }

  @Test
  fun `the placeholder notification is built on demand`() {
    manager.createPlaceholder()

    assertThat(placeholders).hasSize(1)
    verify(exactly = 0) { notificationManager.notify(any(), any()) }
  }

  private fun awaitBuilt(): NotificationData =
    requireNotNull(built.poll(AWAIT_SECONDS, TimeUnit.SECONDS)) { "no notification was built" }

  private fun track(title: String, artist: String = "", coverUrl: String = "") =
    BasicTrackInfo(artist = artist, title = title, coverUrl = coverUrl)

  private companion object {
    const val AWAIT_SECONDS = 5L
    const val MEDIA_NOTIFICATION_ID = AppNotificationManager.MEDIA_SESSION_NOTIFICATION_ID
  }
}
