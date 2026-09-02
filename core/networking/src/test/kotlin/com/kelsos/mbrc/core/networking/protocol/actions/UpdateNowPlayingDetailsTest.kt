package com.kelsos.mbrc.core.networking.protocol.actions

import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.state.TrackDetails
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import com.kelsos.mbrc.core.networking.protocol.base.ProtocolMessage
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Guards the wire contract of the nowplayingdetails message. Give each field a value unique within
 * the payload: a `@Json` name or a copy wired to the wrong property fails silently otherwise,
 * leaving the default `""` behind rather than raising anything.
 */
class UpdateNowPlayingDetailsTest {

  private lateinit var stateHandler: PlayerStateHandler
  private lateinit var action: UpdateNowPlayingDetails

  @Before
  fun setUp() {
    stateHandler = mockk(relaxed = true)
    action = UpdateNowPlayingDetails(Moshi.Builder().build(), stateHandler)
  }

  private suspend fun execute(data: Any): TrackDetails? {
    val message = mockk<ProtocolMessage> {
      every { type } returns Protocol.NowPlayingDetails
      every { this@mockk.data } returns data
    }
    action.execute(message)

    val invocations = mutableListOf<TrackDetails>()
    verify(atLeast = 0) { stateHandler.updateTrackDetails(capture(invocations)) }
    return invocations.singleOrNull()
  }

  @Test
  fun `every wire field lands on its own property`() = runTest {
    val details = execute(
      mapOf(
        "albumArtist" to "an album artist",
        "genre" to "a genre",
        "trackNo" to "3",
        "trackCount" to "12",
        "discNo" to "1",
        "discCount" to "2",
        "grouping" to "a grouping",
        "publisher" to "a publisher",
        "ratingAlbum" to "4",
        "composer" to "a composer",
        "comment" to "a comment",
        "encoder" to "an encoder",
        "kind" to "a kind",
        "format" to "a format",
        "size" to "1024",
        "channels" to "2",
        "sampleRate" to "44100",
        "bitrate" to "320",
        "dateModified" to "2026-01-02",
        "dateAdded" to "2026-01-03",
        "lastPlayed" to "2026-01-04",
        "playCount" to "7",
        "skipCount" to "1",
        "duration" to "215"
      )
    )

    assertThat(details).isNotNull()
    assertThat(details).isEqualTo(
      TrackDetails(
        albumArtist = "an album artist",
        genre = "a genre",
        trackNo = "3",
        trackCount = "12",
        discNo = "1",
        discCount = "2",
        grouping = "a grouping",
        publisher = "a publisher",
        ratingAlbum = "4",
        composer = "a composer",
        comment = "a comment",
        encoder = "an encoder",
        kind = "a kind",
        format = "a format",
        size = "1024",
        channels = "2",
        sampleRate = "44100",
        bitrate = "320",
        dateModified = "2026-01-02",
        dateAdded = "2026-01-03",
        lastPlayed = "2026-01-04",
        playCount = "7",
        skipCount = "1",
        duration = "215"
      )
    )
  }

  @Test
  fun `a partial payload leaves the absent fields empty`() = runTest {
    val details = execute(mapOf("albumArtist" to "an album artist", "duration" to "215"))

    assertThat(details).isNotNull()
    assertThat(details!!.albumArtist).isEqualTo("an album artist")
    assertThat(details.duration).isEqualTo("215")
    assertThat(details.genre).isEmpty()
    assertThat(details.composer).isEmpty()
  }

  @Test
  fun `an empty payload still reports as having no data`() = runTest {
    val details = execute(emptyMap<String, Any?>())

    assertThat(details).isNotNull()
    assertThat(details!!.hasData()).isFalse()
  }

  @Test
  fun `a field the app does not know about is ignored`() = runTest {
    val details = execute(mapOf("genre" to "a genre", "somethingNewerPluginsSend" to "value"))

    assertThat(details).isNotNull()
    assertThat(details!!.genre).isEqualTo("a genre")
  }
}
