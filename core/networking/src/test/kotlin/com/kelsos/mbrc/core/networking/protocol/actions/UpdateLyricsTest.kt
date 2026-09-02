package com.kelsos.mbrc.core.networking.protocol.actions

import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import com.kelsos.mbrc.core.networking.protocol.base.ProtocolMessage
import com.kelsos.mbrc.core.networking.protocol.payloads.LyricsPayload
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Lyrics arrive as HTML fragments and are turned into the lines the player renders.
 *
 * The action's `?: return` guards only a literal JSON null. Moshi throws on a shape mismatch, and
 * nothing in the action catches it, so a malformed payload is contained solely by
 * `MessageHandlerImpl.execute`, which drops the message and logs it.
 */
class UpdateLyricsTest {

  private lateinit var stateHandler: PlayerStateHandler
  private lateinit var action: UpdateLyrics
  private lateinit var written: MutableList<List<String>>

  @Before
  fun setUp() {
    written = mutableListOf()
    stateHandler = mockk(relaxed = true) {
      every { updateLyrics(any()) } answers { written.add(firstArg()) }
    }
    action = UpdateLyrics(Moshi.Builder().build(), stateHandler)
  }

  private fun message(lyrics: String, status: Int = LyricsPayload.SUCCESS): ProtocolMessage =
    mockk {
      every { type } returns Protocol.NowPlayingLyrics
      every { data } returns mapOf("status" to status, "lyrics" to lyrics)
    }

  private val result: List<String> get() = written.single()

  @Test
  fun `paragraph and break tags become line breaks`() = runTest {
    action.execute(message("first<br>second<p>third"))

    assertThat(result).containsExactly("first", "second", "third").inOrder()
  }

  @Test
  fun `escaped entities are decoded`() = runTest {
    action.execute(message("&lt;i&gt; said &quot;no&quot; &amp; left, it&apos;s fine"))

    assertThat(result).containsExactly("""<i> said "no" & left, it's fine""")
  }

  @Test
  fun `an escaped ampersand is decoded last so a nested entity survives as text`() = runTest {
    action.execute(message("&amp;lt;"))

    assertThat(result).containsExactly("&lt;")
  }

  @Test
  fun `surrounding blank space is trimmed`() = runTest {
    action.execute(message("   \r\n a line \r\n   "))

    assertThat(result).containsExactly("a line")
  }

  @Test
  fun `trailing blank lines are dropped but interior ones are kept`() = runTest {
    action.execute(message("first<br><br>second<br><br>"))

    assertThat(result).containsExactly("first", "", "second").inOrder()
  }

  @Test
  fun `a not found response clears the lyrics`() = runTest {
    action.execute(message("ignored", status = LyricsPayload.NOT_FOUND))

    assertThat(result).isEmpty()
  }

  @Test(expected = JsonDataException::class)
  fun `a payload of the wrong shape throws for the dispatcher to contain`() = runTest {
    val message: ProtocolMessage = mockk {
      every { type } returns Protocol.NowPlayingLyrics
      every { data } returns "not a payload"
    }

    action.execute(message)
  }
}
