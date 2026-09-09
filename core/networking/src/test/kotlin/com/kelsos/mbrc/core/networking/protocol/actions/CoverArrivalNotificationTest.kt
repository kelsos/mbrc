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
 * A track change and its cover arrive as two separate messages. These tests pin that sequence end
 * to end, because [#369](https://github.com/musicbeeremote/mbrc/issues/369) was the widget seeing
 * only the first of the two and so showing the previous track's artwork.
 *
 * `published` records what reaches the application state. That is the single source both the in-app
 * player and the widget read from now, so what these assertions describe is what the widget sees.
 */
class CoverArrivalNotificationTest {

  private lateinit var stateHandler: PlayerStateHandler
  private lateinit var coverHandler: CoverHandler
  private lateinit var notifier: TrackChangeNotifier
  private lateinit var trackChange: UpdateNowPlayingTrack
  private lateinit var coverArrival: UpdateCover
  private lateinit var track: MutableStateFlow<TrackInfo>
  private lateinit var published: MutableList<TrackInfo>

  @Before
  fun setUp() {
    val moshi = Moshi.Builder().build()
    track = MutableStateFlow(BasicTrackInfo())
    published = mutableListOf()
    stateHandler = mockk(relaxed = true) {
      every { playingTrack } returns track
      every { updatePlayingTrack(any()) } answers {
        val updated = firstArg<TrackInfo>()
        published.add(updated)
        track.value = updated
      }
    }
    notifier = mockk(relaxed = true)
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
  fun `the state ends up with the cover of the track it belongs to`() = runTest {
    playTrack("first")
    coverArrives("/covers/first.jpg")
    playTrack("second")
    coverArrives("/covers/second.jpg")

    val current = published.last()
    assertThat(current.title).isEqualTo("second")
    assertThat(current.coverUrl).isEqualTo("/covers/second.jpg")
  }

  @Test
  fun `a track change carries the previous cover until its own arrives`() = runTest {
    playTrack("first")
    coverArrives("/covers/first.jpg")

    playTrack("second")

    val latest = published.last()
    assertThat(latest.title).isEqualTo("second")
    assertWithMessage("the cover for the new track has not arrived yet, so the old one is carried")
      .that(latest.coverUrl)
      .isEqualTo("/covers/first.jpg")
  }

  @Test
  fun `a cover arrival publishes the correction that the artwork depends on`() = runTest {
    playTrack("first")
    coverArrives("/covers/first.jpg")
    playTrack("second")

    val publishedBefore = published.size
    coverArrives("/covers/second.jpg")

    assertWithMessage(
      "mbrc#369: the cover message must reach the same state the widget reads, or the artwork " +
        "stays a track behind the title"
    ).that(published.size)
      .isEqualTo(publishedBefore + 1)
    assertThat(published.last().coverUrl).isEqualTo("/covers/second.jpg")
    assertThat(published.last().title).isEqualTo("second")
  }

  @Test
  fun `the first track of a session is published with no cover at all`() = runTest {
    playTrack("first")

    assertThat(published.single().coverUrl).isEmpty()
  }
}
