package com.kelsos.mbrc.core.networking.protocol.actions

import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.state.BasicTrackInfo
import com.kelsos.mbrc.core.common.state.TrackInfo
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import com.kelsos.mbrc.core.networking.protocol.base.ProtocolMessage
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * A track change fans out to four places, and the cover the previous track left behind has to
 * survive the copy: the new cover arrives in a separate message, so clearing it here would blank
 * the artwork on every track change until that message landed.
 *
 * The action's `?: return` guards only a literal JSON null. Moshi throws on a shape mismatch, so a
 * malformed payload is contained solely by `MessageHandlerImpl.execute`.
 */
class UpdateNowPlayingTrackTest {

  private lateinit var stateHandler: PlayerStateHandler
  private lateinit var notifier: TrackChangeNotifier
  private lateinit var action: UpdateNowPlayingTrack
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
    notifier = mockk(relaxed = true)
    action = UpdateNowPlayingTrack(stateHandler, notifier, Moshi.Builder().build())
  }

  private fun message(data: Any): ProtocolMessage = mockk {
    every { type } returns Protocol.NowPlayingTrack
    every { this@mockk.data } returns data
  }

  private fun trackData(
    artist: String = "an artist",
    title: String = "a title",
    album: String = "an album",
    year: String = "1999",
    path: String = "/music/a.mp3"
  ) = mapOf(
    "artist" to artist,
    "title" to title,
    "album" to album,
    "year" to year,
    "path" to path
  )

  @Test
  fun `every field of the track is taken from the message`() = runTest {
    action.execute(message(trackData()))

    val result = written.single()
    assertThat(result.artist).isEqualTo("an artist")
    assertThat(result.title).isEqualTo("a title")
    assertThat(result.album).isEqualTo("an album")
    assertThat(result.year).isEqualTo("1999")
    assertThat(result.path).isEqualTo("/music/a.mp3")
  }

  @Test
  fun `the cover of the previous track is carried over`() = runTest {
    track.value = BasicTrackInfo(coverUrl = "/covers/previous.jpg")

    action.execute(message(trackData()))

    assertThat(written.single().coverUrl).isEqualTo("/covers/previous.jpg")
  }

  @Test
  fun `a track change is published, persisted and followed by a details request`() = runTest {
    action.execute(message(trackData()))

    val published = written.single()
    coVerify { notifier.persistTrackInfo(published) }
    coVerify { notifier.requestTrackDetails() }
  }

  @Test(expected = JsonDataException::class)
  fun `a payload of the wrong shape throws for the dispatcher to contain`() = runTest {
    action.execute(message("not a track"))
  }

  @Test
  fun `missing fields fall back to empty rather than failing`() = runTest {
    action.execute(message(mapOf("title" to "a title")))

    val result = written.single()
    assertThat(result.title).isEqualTo("a title")
    assertThat(result.artist).isEmpty()
    assertThat(result.album).isEmpty()
  }
}
