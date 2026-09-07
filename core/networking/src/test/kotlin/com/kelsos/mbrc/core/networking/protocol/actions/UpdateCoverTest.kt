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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * The cover arrives in its own message after the track change, so this action carries the rest of
 * the track forward and replaces only the cover url.
 */
class UpdateCoverTest {

  private lateinit var stateHandler: PlayerStateHandler
  private lateinit var coverHandler: CoverHandler
  private lateinit var action: UpdateCover
  private lateinit var track: MutableStateFlow<TrackInfo>
  private lateinit var written: MutableList<TrackInfo>

  @Before
  fun setUp() {
    track = MutableStateFlow(BasicTrackInfo())
    written = mutableListOf()
    stateHandler = mockk(relaxed = true) {
      every { playingTrack } returns track
      every { updatePlayingTrack(any()) } answers { written.add(firstArg()) }
    }
    coverHandler = mockk(relaxed = true)
    action = UpdateCover(Moshi.Builder().build(), coverHandler, stateHandler)
  }

  private fun message(data: Any): ProtocolMessage = mockk {
    every { type } returns Protocol.NowPlayingCover
    every { this@mockk.data } returns data
  }

  private fun coverData(status: Int) = mapOf("status" to status)

  @Test
  fun `a ready cover is fetched and its uri published`() = runTest {
    coEvery { coverHandler.fetchAndStoreCover() } returns "/covers/current.jpg"

    action.execute(message(coverData(CoverPayload.READY)))

    assertThat(written.single().coverUrl).isEqualTo("/covers/current.jpg")
  }

  @Test
  fun `a missing cover clears the stored covers and blanks the url`() = runTest {
    track.value = BasicTrackInfo(coverUrl = "/covers/previous.jpg")

    action.execute(message(coverData(CoverPayload.NOT_FOUND)))

    coVerify { coverHandler.clearCovers() }
    assertWithMessage("a stale cover left in place would outlive the track it belongs to")
      .that(written.single().coverUrl)
      .isEmpty()
  }

  @Test
  fun `the rest of the playing track is carried over`() = runTest {
    track.value = BasicTrackInfo(
      artist = "an artist",
      title = "a title",
      album = "an album",
      year = "1999",
      path = "/music/a.mp3"
    )
    coEvery { coverHandler.fetchAndStoreCover() } returns "/covers/current.jpg"

    action.execute(message(coverData(CoverPayload.READY)))

    val result = written.single()
    assertThat(result.artist).isEqualTo("an artist")
    assertThat(result.title).isEqualTo("a title")
    assertThat(result.album).isEqualTo("an album")
    assertThat(result.year).isEqualTo("1999")
    assertThat(result.path).isEqualTo("/music/a.mp3")
  }

  @Test
  fun `an unrecognised status leaves the track untouched`() = runTest {
    action.execute(message(coverData(status = 0)))

    assertThat(written).isEmpty()
    coVerify(exactly = 0) { coverHandler.fetchAndStoreCover() }
    coVerify(exactly = 0) { coverHandler.clearCovers() }
  }

  @Test
  fun `the success status is not a cover status and is ignored`() = runTest {
    action.execute(message(coverData(CoverPayload.SUCCESS)))

    assertWithMessage("only READY and NOT_FOUND carry a cover; SUCCESS is the http-style ack")
      .that(written)
      .isEmpty()
  }

  @Test
  fun `a failed fetch publishes the empty url the handler returns`() = runTest {
    track.value = BasicTrackInfo(coverUrl = "/covers/previous.jpg")
    coEvery { coverHandler.fetchAndStoreCover() } returns ""

    action.execute(message(coverData(CoverPayload.READY)))

    assertThat(written.single().coverUrl).isEmpty()
  }

  @Test
  fun `a status missing from the payload falls back to not found`() = runTest {
    action.execute(message(emptyMap<String, Any>()))

    coVerify { coverHandler.clearCovers() }
    assertThat(written.single().coverUrl).isEmpty()
  }
}
