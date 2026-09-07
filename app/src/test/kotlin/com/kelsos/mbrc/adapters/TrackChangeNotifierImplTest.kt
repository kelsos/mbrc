package com.kelsos.mbrc.adapters

import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.state.AppStatePublisher
import com.kelsos.mbrc.core.common.state.BasicTrackInfo
import com.kelsos.mbrc.core.common.state.PlayerState
import com.kelsos.mbrc.core.common.state.TrackDetails
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.networking.api.PlaybackApi
import com.kelsos.mbrc.core.networking.protocol.payloads.NowPlayingDetailsPayload
import com.kelsos.mbrc.core.platform.state.PlayingTrack
import com.kelsos.mbrc.feature.widgets.WidgetUpdater
import com.kelsos.mbrc.state.PlayingTrackCache
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The adapter that fans a track change out to the widget, the cache and the details request. Every
 * hand-off converts [com.kelsos.mbrc.core.common.state.TrackInfo] to the parcelable [PlayingTrack],
 * so the conversion is asserted rather than assumed.
 */
class TrackChangeNotifierImplTest {

  private val widgetUpdater: WidgetUpdater = mockk(relaxed = true)
  private val cache: PlayingTrackCache = mockk(relaxed = true)
  private val playbackApi: PlaybackApi = mockk()
  private val appState: AppStatePublisher = mockk(relaxed = true)

  private val immediateDispatchers = object : AppCoroutineDispatchers {
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
    override val database: CoroutineDispatcher = Dispatchers.Unconfined
    override val network: CoroutineDispatcher = Dispatchers.Unconfined
  }

  private val notifier = TrackChangeNotifierImpl(
    widgetUpdater,
    cache,
    playbackApi,
    appState,
    immediateDispatchers
  )

  private val track = BasicTrackInfo(
    artist = "an artist",
    title = "a title",
    album = "an album",
    year = "1999",
    path = "/music/a.mp3",
    coverUrl = "/covers/a.jpg",
    duration = 1234
  )

  @Test
  fun `a track change reaches the widget as a playing track`() {
    val sent = mutableListOf<PlayingTrack>()
    every { widgetUpdater.updatePlayingTrack(any()) } answers { sent.add(firstArg()) }

    notifier.notifyTrackChanged(track)

    val result = sent.single()
    assertThat(result.artist).isEqualTo("an artist")
    assertThat(result.title).isEqualTo("a title")
    assertThat(result.album).isEqualTo("an album")
    assertThat(result.coverUrl).isEqualTo("/covers/a.jpg")
    assertThat(result.duration).isEqualTo(1234)
  }

  @Test
  fun `a play state change reaches the widget untouched`() {
    notifier.notifyPlayStateChanged(PlayerState.Playing)

    verify { widgetUpdater.updatePlayState(PlayerState.Playing) }
  }

  @Test
  fun `persisting a track hands the cache the same fields`() = runTest {
    val persisted = mutableListOf<PlayingTrack>()
    coEvery { cache.persistInfo(any()) } answers { persisted.add(firstArg()) }

    notifier.persistTrackInfo(track)

    assertThat(persisted.single().path).isEqualTo("/music/a.mp3")
  }

  @Test
  fun `requested details are published to the app state`() = runTest {
    coEvery { playbackApi.getTrackDetails() } returns NowPlayingDetailsPayload(genre = "jazz")
    val published = mutableListOf<TrackDetails>()
    every { appState.updateTrackDetails(any()) } answers { published.add(firstArg()) }

    notifier.requestTrackDetails()

    assertThat(published.single().genre).isEqualTo("jazz")
  }

  @Test
  fun `a failed details request is swallowed and publishes nothing`() = runTest {
    coEvery { playbackApi.getTrackDetails() } throws RuntimeException("boom")

    notifier.requestTrackDetails()

    verify(exactly = 0) { appState.updateTrackDetails(any()) }
  }
}
