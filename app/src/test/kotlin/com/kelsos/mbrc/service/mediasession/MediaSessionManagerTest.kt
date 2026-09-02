package com.kelsos.mbrc.service.mediasession

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.state.AppState
import com.kelsos.mbrc.core.common.test.testDispatchers
import com.kelsos.mbrc.core.networking.protocol.usecases.UserActionUseCase
import com.kelsos.mbrc.core.networking.protocol.usecases.VolumeModifyUseCase
import io.mockk.mockk
import kotlinx.coroutines.isActive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the session lifecycle: how many sessions exist, and whether the scope handed to the player
 * is usable after a destroy. A stale cancelled scope leaves a reinitialised session silently dead,
 * its player unable to collect anything.
 *
 * A real [androidx.media3.session.MediaSession] is built rather than mocked, because MockK cannot
 * redefine the Media3 class. Do not add `coroutineTestTimeout()` here: Media3 rejects access from
 * any thread but the SDK main thread, and that rule would move the test off it.
 */
@RunWith(AndroidJUnit4::class)
class MediaSessionManagerTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private lateinit var manager: MediaSessionManager

  @Before
  fun setUp() {
    manager = MediaSessionManager(
      context = context,
      userActionUseCase = mockk<UserActionUseCase>(relaxed = true),
      volumeModifyUseCase = mockk<VolumeModifyUseCase>(relaxed = true),
      appState = AppState(),
      dispatchers = testDispatchers
    )
  }

  @After
  fun tearDown() {
    manager.destroy()
  }

  @Test
  fun `there is no session before initialize`() {
    assertThat(manager.mediaSession).isNull()
  }

  @Test
  fun `initialize creates a session and publishes it`() {
    val session = manager.initialize()

    assertThat(manager.mediaSession).isSameInstanceAs(session)
  }

  @Test
  fun `initialize is idempotent`() {
    val first = manager.initialize()
    val second = manager.initialize()

    assertThat(second).isSameInstanceAs(first)
  }

  @Test
  fun `destroy clears the session`() {
    manager.initialize()

    manager.destroy()

    assertThat(manager.mediaSession).isNull()
  }

  @Test
  fun `destroy is safe before anything was initialized`() {
    manager.destroy()

    assertThat(manager.mediaSession).isNull()
  }

  @Test
  fun `destroy cancels the scope handed to the player`() {
    manager.initialize()
    val scope = manager.scope

    manager.destroy()

    assertThat(scope.isActive).isFalse()
  }

  @Test
  fun `reinitializing replaces the cancelled scope rather than reusing it`() {
    manager.initialize()
    manager.destroy()

    manager.initialize()

    assertThat(manager.scope.isActive).isTrue()
  }

  @Test
  fun `initializing again after a destroy gives a different session`() {
    val first = manager.initialize()
    manager.destroy()

    val second = manager.initialize()

    assertThat(second).isNotSameInstanceAs(first)
  }
}
