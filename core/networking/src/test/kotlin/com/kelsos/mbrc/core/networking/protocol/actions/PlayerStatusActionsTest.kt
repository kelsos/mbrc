package com.kelsos.mbrc.core.networking.protocol.actions

import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.state.PlayerState
import com.kelsos.mbrc.core.common.state.PlayerStatusModel
import com.kelsos.mbrc.core.common.state.Repeat
import com.kelsos.mbrc.core.common.state.ShuffleMode
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import com.kelsos.mbrc.core.networking.protocol.base.ProtocolMessage
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * The actions that each own one field of [PlayerStatusModel]. Every one of them reads the previous
 * status, copies a single field onto it and writes it back, so the shared risk is a change that
 * drops one of the fields it was not responsible for.
 */
class PlayerStatusActionsTest {

  private lateinit var stateHandler: PlayerStateHandler
  private lateinit var status: MutableStateFlow<PlayerStatusModel>
  private lateinit var written: MutableList<PlayerStatusModel>

  /** A status with every field set away from its default, so a dropped field is visible. */
  private val populated = PlayerStatusModel(
    volume = 42,
    mute = true,
    shuffle = ShuffleMode.AutoDJ,
    scrobbling = true,
    repeat = Repeat.All,
    state = PlayerState.Playing
  )

  @Before
  fun setUp() {
    status = MutableStateFlow(populated)
    written = mutableListOf()
    stateHandler = mockk(relaxed = true) {
      every { playerStatus } returns status
      every { updatePlayerStatus(any()) } answers { written.add(firstArg()) }
    }
  }

  private fun message(data: Any): ProtocolMessage = mockk {
    every { type } returns Protocol.PlayerStatus
    every { this@mockk.data } returns data
  }

  private val result: PlayerStatusModel get() = written.single()

  @Test
  fun `mute is taken from the message and everything else is kept`() = runTest {
    UpdateMute(stateHandler).execute(message(false))

    assertThat(result).isEqualTo(populated.copy(mute = false))
  }

  @Test
  fun `scrobbling is taken from the message and everything else is kept`() = runTest {
    UpdateLastFm(stateHandler).execute(message(false))

    assertThat(result).isEqualTo(populated.copy(scrobbling = false))
  }

  @Test
  fun `repeat is taken from the message and everything else is kept`() = runTest {
    UpdateRepeat(stateHandler).execute(message("none"))

    assertThat(result).isEqualTo(populated.copy(repeat = Repeat.None))
  }

  @Test
  fun `shuffle is taken from the message and everything else is kept`() = runTest {
    UpdateShuffle(stateHandler).execute(message("off"))

    assertThat(result).isEqualTo(populated.copy(shuffle = ShuffleMode.Off))
  }

  @Test
  fun `volume is taken from the message and everything else is kept`() = runTest {
    UpdateVolume(stateHandler).execute(message(80))

    assertThat(result).isEqualTo(populated.copy(volume = 80))
  }

  @Test
  fun `a volume sent as a floating point number is truncated`() = runTest {
    UpdateVolume(stateHandler).execute(message(80.7))

    assertThat(result.volume).isEqualTo(80)
  }

  @Test
  fun `a volume sent as a string is parsed like a number`() = runTest {
    UpdateVolume(stateHandler).execute(message("80"))

    assertThat(result).isEqualTo(populated.copy(volume = 80))
  }

  @Test
  fun `a volume sent as a decimal string is truncated`() = runTest {
    UpdateVolume(stateHandler).execute(message("80.7"))

    assertThat(result.volume).isEqualTo(80)
  }

  @Test
  fun `a volume above the maximum is clamped`() = runTest {
    UpdateVolume(stateHandler).execute(message(150))

    assertThat(result.volume).isEqualTo(100)
  }

  @Test
  fun `a negative volume is clamped`() = runTest {
    UpdateVolume(stateHandler).execute(message(-20))

    assertThat(result.volume).isEqualTo(0)
  }

  @Test
  fun `an out of range volume in a full status update is clamped`() = runTest {
    UpdatePlayerStatus(stateHandler, Moshi.Builder().build()).execute(
      message(
        mapOf(
          Protocol.PLAYER_VOLUME to 150,
          Protocol.PLAYER_STATE to "playing",
          Protocol.PLAYER_MUTE to false,
          Protocol.PLAYER_REPEAT to "none",
          Protocol.PLAYER_SHUFFLE to "off",
          Protocol.PLAYER_SCROBBLE to false
        )
      )
    )

    assertThat(result.volume).isEqualTo(100)
  }

  @Test
  fun `an unparseable volume leaves the status untouched`() = runTest {
    UpdateVolume(stateHandler).execute(message("loud"))

    assertThat(written).isEmpty()
  }

  @Test
  fun `an unparseable volume does not take the protocol handler down`() = runTest {
    listOf<Any>("", "null", "loud", emptyList<String>()).forEach { data ->
      UpdateVolume(stateHandler).execute(message(data))
    }

    assertThat(written).isEmpty()
  }

  @Test
  fun `play state is taken from the message and everything else is kept`() = runTest {
    val notifier: TrackChangeNotifier = mockk(relaxed = true)

    UpdatePlayState(stateHandler, notifier).execute(message("paused"))

    assertThat(result).isEqualTo(populated.copy(state = PlayerState.Paused))
  }

  @Test
  fun `an unknown repeat value falls back rather than throwing`() = runTest {
    UpdateRepeat(stateHandler).execute(message("nonsense"))

    assertThat(written).hasSize(1)
    assertThat(result.volume).isEqualTo(populated.volume)
  }

  @Test
  fun `an absent previous status starts from the defaults`() = runTest {
    every { stateHandler.playerStatus } returns MutableStateFlow(PlayerStatusModel())

    UpdateMute(stateHandler).execute(message(true))

    assertThat(result).isEqualTo(PlayerStatusModel(mute = true))
  }
}
