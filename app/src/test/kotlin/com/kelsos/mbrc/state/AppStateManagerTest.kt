package com.kelsos.mbrc.state

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.core.common.state.AppState
import com.kelsos.mbrc.core.common.state.BasicTrackInfo
import com.kelsos.mbrc.core.common.state.ConnectionStateFlow
import com.kelsos.mbrc.core.common.state.ConnectionStatus
import com.kelsos.mbrc.core.common.state.PlayerState
import com.kelsos.mbrc.core.common.state.PlayerStatusModel
import com.kelsos.mbrc.core.common.state.PlayingPosition
import com.kelsos.mbrc.core.common.state.TrackInfo
import com.kelsos.mbrc.core.common.test.coroutineTestTimeout
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.common.test.testDispatchers
import com.kelsos.mbrc.service.ServiceLifecycleManager
import com.kelsos.mbrc.service.mediasession.AppNotificationManager
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

/**
 * Rules for adding a test here: drive it through [managerTest] rather than `runTest`, move time
 * with [elapse] or [idle] rather than `TestScope`, and never call [idle] while the position updater
 * is running.
 */
class AppStateManagerTest {

  private val appState = AppState()
  private val notifications: AppNotificationManager = mockk(relaxed = true)
  private val serviceLifecycleManager: ServiceLifecycleManager = mockk(relaxed = true)
  private val connection = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Offline)

  private val connectionState = object : ConnectionStateFlow {
    override val connection: StateFlow<ConnectionStatus> = this@AppStateManagerTest.connection
      .asStateFlow()
    override val isConnected: Boolean
      get() = connection.value == ConnectionStatus.Connected
  }

  private var restoredTrack: TrackInfo = BasicTrackInfo()
  private val persisted = mutableListOf<TrackInfo>()

  private val trackCache = object : PlayingTrackCache {
    override suspend fun persistInfo(playingTrack: TrackInfo) {
      persisted.add(playingTrack)
    }

    override suspend fun restoreInfo(): TrackInfo = restoredTrack
  }

  private lateinit var stateManager: AppStateManager

  @get:Rule
  val timeout: Timeout = coroutineTestTimeout()

  @Before
  fun setUp() {
    stateManager = AppStateManager(
      appState = appState,
      connectionState = connectionState,
      notifications = notifications,
      trackCache = trackCache,
      serviceLifecycleManager = serviceLifecycleManager,
      dispatchers = testDispatchers
    )
  }

  @After
  fun tearDown() {
    stateManager.stop()
  }

  /**
   * Runs [body] with the manager stopped even on failure. A live position updater keeps the
   * scheduler from ever going idle, which `runTest` waits for on its way out, so a `stop()` placed
   * after an assertion hangs the build instead of failing. `@After` runs too late to help.
   */
  private fun managerTest(body: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
    try {
      body()
    } finally {
      stateManager.stop()
      idle()
    }
  }

  private fun idle() = testDispatcher.scheduler.advanceUntilIdle()

  private fun elapse(millis: Long) = testDispatcher.scheduler.advanceTimeBy(millis)

  /**
   * Publishes [state] and expires the debounce ahead of its collector. The extra millisecond
   * matters: `advanceTimeBy` runs only what is scheduled strictly before the new time.
   */
  private fun publishPlayerState(state: PlayerState) {
    appState.updatePlayerStatus(PlayerStatusModel(state = state))
    elapse(PLAYER_STATE_DEBOUNCE_MS + 1)
  }

  @Test
  fun `the last played track is restored on construction`() = managerTest {
    restoredTrack = BasicTrackInfo(artist = "an artist", title = "a title")
    stateManager = AppStateManager(
      appState = appState,
      connectionState = connectionState,
      notifications = notifications,
      trackCache = trackCache,
      serviceLifecycleManager = serviceLifecycleManager,
      dispatchers = testDispatchers
    )

    idle()

    assertThat(appState.playingTrack.value.artist).isEqualTo("an artist")
    assertThat(appState.playingTrack.value.title).isEqualTo("a title")
  }

  @Test
  fun `a second start does not attach a second set of collectors`() = managerTest {
    stateManager.start()
    stateManager.start()
    idle()

    val track = BasicTrackInfo(artist = "an artist")
    appState.updatePlayingTrack(track)
    idle()

    verify(exactly = 1) { notifications.updatePlayingTrack(track) }
    assertWithMessage("a second start would attach a second collector, delivering it twice")
      .that(persisted.count { it == track })
      .isEqualTo(1)
  }

  @Test
  fun `going offline before anything connected does not report a lost connection`() = managerTest {
    stateManager.start()
    idle()

    verify(exactly = 0) { serviceLifecycleManager.onConnectionLost() }
  }

  @Test
  fun `going offline after a connection reports a lost connection`() = managerTest {
    stateManager.start()
    idle()

    connection.value = ConnectionStatus.Connected
    idle()
    connection.value = ConnectionStatus.Offline
    idle()

    verify(exactly = 1) { serviceLifecycleManager.onConnectionLost() }
  }

  @Test
  fun `a denied local network is not treated as a lost connection`() = managerTest {
    stateManager.start()
    idle()

    connection.value = ConnectionStatus.Connected
    idle()
    connection.value = ConnectionStatus.LocalNetworkDenied
    idle()

    verify(exactly = 0) { serviceLifecycleManager.onConnectionLost() }
  }

  @Test
  fun `the notification follows the connection state`() = managerTest {
    stateManager.start()
    idle()

    connection.value = ConnectionStatus.Connected
    idle()
    connection.value = ConnectionStatus.Offline
    idle()

    verify { notifications.connectionStateChanged(true) }
    verify { notifications.connectionStateChanged(false) }
  }

  @Test
  fun `playing advances the position once per second`() = managerTest {
    appState.updatePlayingPosition(PlayingPosition(current = 0, total = 10_000))
    stateManager.start()
    idle()

    publishPlayerState(PlayerState.Playing)
    elapse(3 * ONE_SECOND)

    assertThat(appState.playingPosition.value.current).isEqualTo(3 * ONE_SECOND)
  }

  @Test
  fun `the position never runs past the end of a track`() = managerTest {
    appState.updatePlayingPosition(PlayingPosition(current = 0, total = 2_500))
    stateManager.start()
    idle()

    publishPlayerState(PlayerState.Playing)
    elapse(10 * ONE_SECOND)

    assertThat(appState.playingPosition.value.current).isEqualTo(2_500)
  }

  @Test
  fun `a stream keeps counting past a total it does not have`() = managerTest {
    appState.updatePlayingPosition(PlayingPosition(current = 0, total = -1))
    stateManager.start()
    idle()

    publishPlayerState(PlayerState.Playing)
    elapse(5 * ONE_SECOND)

    assertThat(appState.playingPosition.value.current).isEqualTo(5 * ONE_SECOND)
  }

  @Test
  fun `pausing stops the position from advancing`() = managerTest {
    appState.updatePlayingPosition(PlayingPosition(current = 0, total = 60_000))
    stateManager.start()
    idle()

    publishPlayerState(PlayerState.Playing)
    elapse(2 * ONE_SECOND)
    val whilePlaying = appState.playingPosition.value.current

    publishPlayerState(PlayerState.Paused)
    elapse(5 * ONE_SECOND)

    assertThat(whilePlaying).isEqualTo(2 * ONE_SECOND)
    assertThat(appState.playingPosition.value.current).isEqualTo(whilePlaying)
  }

  @Test
  fun `stopping cancels the notification and the position updater`() = managerTest {
    appState.updatePlayingPosition(PlayingPosition(current = 0, total = 60_000))
    stateManager.start()
    idle()

    publishPlayerState(PlayerState.Playing)
    elapse(2 * ONE_SECOND)

    stateManager.stop()
    val atStop = appState.playingPosition.value.current
    elapse(5 * ONE_SECOND)

    verify { notifications.cancel() }
    assertThat(appState.playingPosition.value.current).isEqualTo(atStop)
  }

  @Test
  fun `a track change is both published and cached`() = managerTest {
    stateManager.start()
    idle()

    val track = BasicTrackInfo(artist = "an artist", title = "a title")
    appState.updatePlayingTrack(track)
    idle()

    verify { notifications.updatePlayingTrack(track) }
    assertThat(persisted).contains(track)
  }

  private companion object {
    const val PLAYER_STATE_DEBOUNCE_MS = 600L
    const val ONE_SECOND = 1_000L
  }
}
