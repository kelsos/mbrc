package com.kelsos.mbrc.core.networking.client

import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The queue is a `MutableSharedFlow` with no replay, so a message queued while nothing is
 * collecting is dropped rather than held. That is the shape behind the silent command drops in
 * #332, and it is a constraint the reconnect path has to work around rather than a bug to fix here.
 */
class MessageQueueTest {

  private val queue = MessageQueueImpl()

  private fun message(protocol: Protocol) = SocketMessage.create(protocol, "")

  @Test
  fun `a queued message reaches a collector`() = runTest {
    val received = collect()

    queue.queue(message(Protocol.PlayerPlay))
    runCurrent()

    assertThat(received).containsExactly(Protocol.PlayerPlay.context)
  }

  @Test
  fun `messages arrive in the order they were queued`() = runTest {
    val received = collect()

    queue.queue(message(Protocol.PlayerPlay))
    queue.queue(message(Protocol.PlayerPause))
    queue.queue(message(Protocol.PlayerNext))
    runCurrent()

    assertThat(received).containsExactly(
      Protocol.PlayerPlay.context,
      Protocol.PlayerPause.context,
      Protocol.PlayerNext.context
    ).inOrder()
  }

  @Test
  fun `a message queued with no collector is dropped, not replayed`() = runTest {
    queue.queue(message(Protocol.PlayerPlay))
    runCurrent()

    val received = collect()
    queue.queue(message(Protocol.PlayerPause))
    runCurrent()

    assertThat(received).containsExactly(Protocol.PlayerPause.context)
  }

  @Test
  fun `every collector receives the same message`() = runTest {
    val first = collect()
    val second = collect()

    queue.queue(message(Protocol.PlayerPlay))
    runCurrent()

    assertThat(first).containsExactly(Protocol.PlayerPlay.context)
    assertThat(second).containsExactly(Protocol.PlayerPlay.context)
  }

  /**
   * Starts a collector on [backgroundScope] and returns the list it fills. The scope matters: the
   * queue never completes, so a collector on the test's own scope would keep it from ever ending.
   */
  private fun kotlinx.coroutines.test.TestScope.collect(): List<String> {
    val received = mutableListOf<String>()
    backgroundScope.launch {
      queue.messages.collect { received.add(it.context) }
    }
    runCurrent()
    return received
  }
}
