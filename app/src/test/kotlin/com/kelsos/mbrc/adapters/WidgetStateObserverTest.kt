package com.kelsos.mbrc.adapters

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.core.common.state.AppState
import com.kelsos.mbrc.core.common.state.BasicTrackInfo
import com.kelsos.mbrc.core.common.state.PlayerState
import com.kelsos.mbrc.core.common.state.PlayerStatusModel
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.platform.state.PlayingTrack
import com.kelsos.mbrc.feature.widgets.WidgetUpdater
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The widget is fed from the application state rather than from a one-shot callback, so that the
 * cover message corrects artwork the track message could not yet know
 * ([#369](https://github.com/musicbeeremote/mbrc/issues/369)).
 */
class WidgetStateObserverTest {

  private val widgetUpdater: WidgetUpdater = mockk(relaxed = true)
  private val appState = AppState()

  private val immediateDispatchers = object : AppCoroutineDispatchers {
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
    override val database: CoroutineDispatcher = Dispatchers.Unconfined
    override val network: CoroutineDispatcher = Dispatchers.Unconfined
  }

  private fun observe(scope: TestScope): MutableList<PlayingTrack> {
    val sent = mutableListOf<PlayingTrack>()
    coEvery { widgetUpdater.updatePlayingTrack(any()) } answers { sent.add(firstArg()) }
    WidgetStateObserver(appState, widgetUpdater, immediateDispatchers).start(scope)
    return sent
  }

  @Test
  fun `a cover arriving after its track reaches the widget`() = runTest {
    val sent = observe(this)

    appState.updatePlayingTrack(
      BasicTrackInfo(artist = "an artist", title = "a title", coverUrl = "/covers/previous.jpg")
    )
    appState.updatePlayingTrack(
      BasicTrackInfo(artist = "an artist", title = "a title", coverUrl = "/covers/current.jpg")
    )

    assertWithMessage("mbrc#369: the widget must see the cover correction, not only the track")
      .that(sent.last().coverUrl)
      .isEqualTo("/covers/current.jpg")
    assertThat(sent.last().title).isEqualTo("a title")

    coroutineContext.cancelChildren()
  }

  @Test
  fun `a track reaches the widget with every field converted`() = runTest {
    val sent = observe(this)

    appState.updatePlayingTrack(
      BasicTrackInfo(
        artist = "an artist",
        title = "a title",
        album = "an album",
        year = "1999",
        path = "/music/a.mp3",
        coverUrl = "/covers/a.jpg",
        duration = 1234
      )
    )

    val result = sent.last()
    assertThat(result.artist).isEqualTo("an artist")
    assertThat(result.title).isEqualTo("a title")
    assertThat(result.album).isEqualTo("an album")
    assertThat(result.coverUrl).isEqualTo("/covers/a.jpg")
    assertThat(result.duration).isEqualTo(1234)

    coroutineContext.cancelChildren()
  }

  @Test
  fun `only a change of play state reaches the widget`() = runTest {
    val states = mutableListOf<PlayerState>()
    coEvery { widgetUpdater.updatePlayState(any()) } answers { states.add(firstArg()) }
    WidgetStateObserver(appState, widgetUpdater, immediateDispatchers).start(this)

    appState.updatePlayerStatus(PlayerStatusModel(state = PlayerState.Playing))
    appState.updatePlayerStatus(PlayerStatusModel(state = PlayerState.Playing, volume = 50))
    appState.updatePlayerStatus(PlayerStatusModel(state = PlayerState.Paused))

    assertWithMessage("a volume change is not a play state change")
      .that(states)
      .containsExactly(PlayerState.Playing, PlayerState.Paused)
      .inOrder()

    coroutineContext.cancelChildren()
  }

  @Test
  fun `the uninitialized state is not pushed to the widget`() = runTest {
    val sent = observe(this)

    assertWithMessage(
      "an empty track would blank the widget and delete its cached cover, and the widget keeps " +
        "its own content across process death"
    ).that(sent)
      .isEmpty()

    coroutineContext.cancelChildren()
  }

  @Test
  fun `the initial undefined play state is not pushed to the widget`() = runTest {
    val states = mutableListOf<PlayerState>()
    coEvery { widgetUpdater.updatePlayState(any()) } answers { states.add(firstArg()) }

    WidgetStateObserver(appState, widgetUpdater, immediateDispatchers).start(this)

    assertWithMessage("anything but Playing renders as paused, so Undefined would flip the icon")
      .that(states)
      .isEmpty()

    coroutineContext.cancelChildren()
  }

  @Test
  fun `an update the widget cannot show does not refresh it`() = runTest {
    val sent = observe(this)
    val track = BasicTrackInfo(artist = "an artist", title = "a title", coverUrl = "/covers/a.jpg")

    appState.updatePlayingTrack(track)
    appState.updatePlayingTrack(track.copy(duration = 1234))
    appState.updatePlayingTrack(track.copy(duration = 1234, path = "/music/a.mp3"))

    assertWithMessage("duration and path are not drawn, so they must not rewrite every widget")
      .that(sent)
      .hasSize(1)

    coroutineContext.cancelChildren()
  }
}
