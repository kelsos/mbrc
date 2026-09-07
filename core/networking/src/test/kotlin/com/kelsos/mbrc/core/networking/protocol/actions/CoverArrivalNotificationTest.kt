package com.kelsos.mbrc.core.networking.protocol.actions

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.core.common.state.BasicTrackInfo
import com.kelsos.mbrc.core.common.state.TrackInfo
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import com.kelsos.mbrc.core.networking.protocol.base.ProtocolMessage
import com.kelsos.mbrc.core.networking.protocol.payloads.CoverPayload
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * A track change and its cover arrive as two separate messages, and only the first of them notifies
 * the widget. These tests pin that sequence end to end so the gap behind
 * [#369](https://github.com/musicbeeremote/mbrc/issues/369) is visible in the suite rather than only
 * on a device.
 *
 * `notified` records what the widget was told; `published` records what the in-app state received.
 * The two diverging is the bug.
 */
class CoverArrivalNotificationTest {

  private lateinit var stateHandler: PlayerStateHandler
  private lateinit var coverHandler: CoverHandler
  private lateinit var notifier: TrackChangeNotifier
  private lateinit var trackChange: UpdateNowPlayingTrack
  private lateinit var coverArrival: UpdateCover
  private lateinit var track: MutableStateFlow<TrackInfo>
  private lateinit var published: MutableList<TrackInfo>
  private lateinit var notified: MutableList<TrackInfo>

  @Before
  fun setUp() {
    val moshi = Moshi.Builder().build()
    track = MutableStateFlow(BasicTrackInfo())
    published = mutableListOf()
    notified = mutableListOf()
    stateHandler = mockk(relaxed = true) {
      every { playingTrack } returns track
      every { updatePlayingTrack(any()) } answers {
        val updated = firstArg<TrackInfo>()
        published.add(updated)
        track.value = updated
      }
    }
    notifier = mockk(relaxed = true) {
      every { notifyTrackChanged(any()) } answers { notified.add(firstArg()) }
    }
    coverHandler = mockk(relaxed = true)
    trackChange = UpdateNowPlayingTrack(stateHandler, notifier, moshi)
    coverArrival = UpdateCover(moshi, coverHandler, stateHandler)
  }

  private fun message(protocol: Protocol, data: Any): ProtocolMessage = mockk {
    every { type } returns protocol
    every { this@mockk.data } returns data
  }

  private suspend fun playTrack(title: String) {
    trackChange.execute(
      message(Protocol.NowPlayingTrack, mapOf("artist" to "an artist", "title" to title))
    )
  }

  private suspend fun coverArrives(url: String) {
    coEvery { coverHandler.fetchAndStoreCover() } returns url
    coverArrival.execute(
      message(Protocol.NowPlayingCover, mapOf("status" to CoverPayload.READY))
    )
  }

  @Test
  fun `the in-app state ends up with the cover of the track it belongs to`() = runTest {
    playTrack("first")
    coverArrives("/covers/first.jpg")
    playTrack("second")
    coverArrives("/covers/second.jpg")

    val current = published.last()
    assertThat(current.title).isEqualTo("second")
    assertThat(current.coverUrl).isEqualTo("/covers/second.jpg")
  }

  @Test
  fun `the widget is told about the new track paired with the previous cover`() = runTest {
    playTrack("first")
    coverArrives("/covers/first.jpg")

    playTrack("second")

    val latest = notified.last()
    assertThat(latest.title).isEqualTo("second")
    assertWithMessage("the cover for the new track has not arrived yet, so the old one is carried")
      .that(latest.coverUrl)
      .isEqualTo("/covers/first.jpg")
  }

  @Test
  fun `a cover arrival never notifies the widget, so its artwork stays a track behind`() = runTest {
    playTrack("first")
    coverArrives("/covers/first.jpg")
    playTrack("second")

    val notificationsBefore = notified.size
    coverArrives("/covers/second.jpg")

    assertWithMessage(
      "mbrc#369: UpdateCover only reaches appState, so nothing corrects the widget artwork. " +
        "When the widget is routed off appState this becomes an outdated expectation and the " +
        "widget should see /covers/second.jpg"
    ).that(notified.size)
      .isEqualTo(notificationsBefore)
    assertThat(notified.last().coverUrl).isEqualTo("/covers/first.jpg")
  }

  @Test
  fun `the first track of a session reaches the widget with no cover at all`() = runTest {
    playTrack("first")

    assertThat(notified.single().coverUrl).isEmpty()
  }
}
