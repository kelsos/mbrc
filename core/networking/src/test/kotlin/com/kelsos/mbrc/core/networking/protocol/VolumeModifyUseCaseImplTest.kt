package com.kelsos.mbrc.core.networking.protocol

import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.state.AppStateFlow
import com.kelsos.mbrc.core.common.state.PlayerStatusModel
import com.kelsos.mbrc.core.common.test.coroutineTestTimeout
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.common.test.testDispatchers
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.networking.client.MessageQueue
import com.kelsos.mbrc.core.networking.client.SocketMessage
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import com.kelsos.mbrc.core.networking.protocol.usecases.VolumeModifyUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

class VolumeModifyUseCaseImplTest {
  @get:Rule
  val timeout: Timeout = coroutineTestTimeout()

  private lateinit var appStateFlow: AppStateFlow
  private lateinit var messageQueue: MessageQueue
  private lateinit var volumeModifyUseCase: VolumeModifyUseCaseImpl

  private val playerStatusFlow = MutableStateFlow(PlayerStatusModel())

  @Before
  fun setUp() {
    appStateFlow = mockk()
    messageQueue = mockk(relaxed = true)
    every { appStateFlow.playerStatus } returns playerStatusFlow
    volumeModifyUseCase = VolumeModifyUseCaseImpl(appStateFlow, messageQueue, testDispatchers)
  }

  // ==================== increase() tests ====================

  @Test
  fun `increase should add step when volume is at 0`() = runTest {
    // Given
    playerStatusFlow.value = PlayerStatusModel(volume = 0)

    // When
    volumeModifyUseCase.increase()

    // Then
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.context).isEqualTo(Protocol.PLAYER_VOLUME)
    assertThat(slot.captured.data).isEqualTo(10)
  }

  @Test
  fun `increase should add step when volume is divisible by step`() = runTest {
    // Given
    playerStatusFlow.value = PlayerStatusModel(volume = 50)

    // When
    volumeModifyUseCase.increase()

    // Then
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.data).isEqualTo(60)
  }

  @Test
  fun `increase should round up to next step when mod is less than half step`() = runTest {
    // Given - volume 52, mod = 2, less than half step (5)
    playerStatusFlow.value = PlayerStatusModel(volume = 52)

    // When
    volumeModifyUseCase.increase()

    // Then - should go to 60 (52 + (10 - 2) = 60)
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.data).isEqualTo(60)
  }

  @Test
  fun `increase should round up to next double step when mod is greater than half step`() =
    runTest {
      // Given - volume 57, mod = 7, greater than half step (5)
      playerStatusFlow.value = PlayerStatusModel(volume = 57)

      // When
      volumeModifyUseCase.increase()

      // Then - should go to 70 (57 + (20 - 7) = 70)
      val slot = slot<SocketMessage>()
      coVerify { messageQueue.queue(capture(slot)) }
      assertThat(slot.captured.data).isEqualTo(70)
    }

  @Test
  fun `increase should cap at max volume when near max`() = runTest {
    // Given
    playerStatusFlow.value = PlayerStatusModel(volume = 95)

    // When
    volumeModifyUseCase.increase()

    // Then
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.data).isEqualTo(100)
  }

  @Test
  fun `increase should stay at max when already at max`() = runTest {
    // Given
    playerStatusFlow.value = PlayerStatusModel(volume = 100)

    // When
    volumeModifyUseCase.increase()

    // Then
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.data).isEqualTo(100)
  }

  // ==================== decrease() tests ====================

  @Test
  fun `decrease should subtract step when volume is at 100`() = runTest {
    // Given
    playerStatusFlow.value = PlayerStatusModel(volume = 100)

    // When
    volumeModifyUseCase.decrease()

    // Then
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.data).isEqualTo(90)
  }

  @Test
  fun `decrease should subtract step when volume is divisible by step`() = runTest {
    // Given
    playerStatusFlow.value = PlayerStatusModel(volume = 50)

    // When
    volumeModifyUseCase.decrease()

    // Then
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.data).isEqualTo(40)
  }

  @Test
  fun `decrease should round down when mod is less than half step`() = runTest {
    // Given - volume 52, mod = 2, less than half step (5)
    playerStatusFlow.value = PlayerStatusModel(volume = 52)

    // When
    volumeModifyUseCase.decrease()

    // Then - should go to 40 (52 - (10 + 2) = 40)
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.data).isEqualTo(40)
  }

  @Test
  fun `decrease should round down to step boundary when mod is greater than half step`() = runTest {
    // Given - volume 57, mod = 7, greater than half step (5)
    playerStatusFlow.value = PlayerStatusModel(volume = 57)

    // When
    volumeModifyUseCase.decrease()

    // Then - should go to 50 (57 - 7 = 50)
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.data).isEqualTo(50)
  }

  @Test
  fun `decrease should floor at min volume when near min`() = runTest {
    // Given
    playerStatusFlow.value = PlayerStatusModel(volume = 5)

    // When
    volumeModifyUseCase.decrease()

    // Then
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.data).isEqualTo(0)
  }

  @Test
  fun `decrease should stay at min when already at min`() = runTest {
    // Given
    playerStatusFlow.value = PlayerStatusModel(volume = 0)

    // When
    volumeModifyUseCase.decrease()

    // Then
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.data).isEqualTo(0)
  }

  // ==================== reduceVolume() tests ====================

  @Test
  fun `reduceVolume should reduce to 20 percent`() = runTest {
    // Given
    playerStatusFlow.value = PlayerStatusModel(volume = 100, mute = false)

    // When
    volumeModifyUseCase.reduceVolume()

    // Then
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.data).isEqualTo(20)
  }

  @Test
  fun `reduceVolume should reduce 50 to 10`() = runTest {
    // Given
    playerStatusFlow.value = PlayerStatusModel(volume = 50, mute = false)

    // When
    volumeModifyUseCase.reduceVolume()

    // Then
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.data).isEqualTo(10)
  }

  @Test
  fun `reduceVolume should not send message when muted`() = runTest {
    // Given
    playerStatusFlow.value = PlayerStatusModel(volume = 50, mute = true)

    // When
    volumeModifyUseCase.reduceVolume()

    // Then
    coVerify(exactly = 0) { messageQueue.queue(any()) }
  }

  @Test
  fun `reduceVolume should not send message when volume is zero`() = runTest {
    // Given
    playerStatusFlow.value = PlayerStatusModel(volume = 0, mute = false)

    // When
    volumeModifyUseCase.reduceVolume()

    // Then
    coVerify(exactly = 0) { messageQueue.queue(any()) }
  }

  // ==================== fire-and-forget tests ====================

  @Test
  fun `tryIncrease should queue the same volume increase does`() = runTest(testDispatcher) {
    // Given
    playerStatusFlow.value = PlayerStatusModel(volume = 20)

    // When
    volumeModifyUseCase.tryIncrease()
    advanceUntilIdle()

    // Then
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.data).isEqualTo(30)
  }

  @Test
  fun `tryDecrease should queue the same volume decrease does`() = runTest(testDispatcher) {
    // Given
    playerStatusFlow.value = PlayerStatusModel(volume = 20)

    // When
    volumeModifyUseCase.tryDecrease()
    advanceUntilIdle()

    // Then
    val slot = slot<SocketMessage>()
    coVerify { messageQueue.queue(capture(slot)) }
    assertThat(slot.captured.data).isEqualTo(10)
  }

  @Test
  fun `tryIncrease should return while the queue is still parked`() = runTest {
    // The queue suspends until something drains it, which is the state the app is in whenever the
    // connection is down. A caller on the main thread must not wait for that. Running the work on
    // an unconfined dispatcher means it starts inline and reaches the suspension point before
    // tryIncrease returns, so if it were awaiting the send this test would never finish.
    val dispatchers = object : AppCoroutineDispatchers {
      override val main: CoroutineDispatcher = UnconfinedTestDispatcher()
      override val io: CoroutineDispatcher = UnconfinedTestDispatcher()
      override val database: CoroutineDispatcher = UnconfinedTestDispatcher()
      override val network: CoroutineDispatcher = UnconfinedTestDispatcher()
    }
    val neverDrained = mockk<MessageQueue>()
    coEvery { neverDrained.queue(any()) } coAnswers { awaitCancellation() }
    playerStatusFlow.value = PlayerStatusModel(volume = 20)
    val useCase = VolumeModifyUseCaseImpl(appStateFlow, neverDrained, dispatchers)

    // When
    useCase.tryIncrease()

    // Then reaching this line at all is the assertion: the send is in flight, not awaited.
    coVerify { neverDrained.queue(any()) }
  }
}
