package com.kelsos.mbrc.core.networking.client

import com.kelsos.mbrc.core.networking.protocol.Clock
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import timber.log.Timber

/**
 * Holds commands that could not be written to the socket so they can be replayed once the
 * connection is restored.
 *
 * Three rules keep a replay from surprising the user:
 * - **Bounded lifetime.** A command older than [ttlMs] is dropped instead of replayed. The age is
 *   measured from the moment the user issued it, so a command that keeps failing on a flapping
 *   connection eventually ages out instead of being refreshed forever.
 * - **Collapsing of value-setting commands.** Contexts in [LAST_WRITE_WINS] describe a value rather
 *   than an event, so only the newest matters: a volume drag replays the final level, and repeated
 *   play/pause taps replay as a single toggle. Event commands such as `next` are never collapsed,
 *   because two of them mean the user asked for two things.
 * - **Only player commands are replayed at all.** [REPLAYABLE] is an allow-list: handshake and
 *   keep-alive traffic belongs to the socket it was queued for, and anything addressed at a queue
 *   position or at whatever is currently playing would land somewhere else by the time it replays.
 *
 * Buffering a command is silent: the connection indicator already tells the user they are offline,
 * and the command is still expected to happen. Discarding one is not, because that is the only
 * point at which a tap is lost with nothing else to signal it, so [onDiscarded] is invoked with the
 * number of commands given up on.
 */
class PendingCommandBuffer(
  private val clock: Clock,
  private val ttlMs: Long = DEFAULT_TTL_MS,
  private val capacity: Int = DEFAULT_CAPACITY,
  private val onDiscarded: (count: Int) -> Unit = {}
) {
  private data class Pending(val message: SocketMessage, val queuedAt: Long)

  private val pending = ArrayDeque<Pending>()

  private var discardedSinceNotify = 0

  /**
   * Buffers [message] for replay. Returns false when the message is protocol chatter that must not
   * survive the connection it was queued for.
   */
  fun stash(message: SocketMessage): Boolean {
    val accepted = stashLocked(message)
    reportDiscarded()
    return accepted
  }

  /**
   * Returns the commands that are still fresh enough to replay, oldest first, and empties the
   * buffer.
   */
  fun drain(): List<SocketMessage> {
    val replayable = drainLocked()
    reportDiscarded()
    return replayable
  }

  @Synchronized
  fun clear() {
    pending.clear()
    // An explicit disconnect is the user's own doing, so anything still buffered is not a loss
    // worth reporting.
    discardedSinceNotify = 0
  }

  @Synchronized
  fun size(): Int = pending.size

  @Synchronized
  private fun stashLocked(message: SocketMessage): Boolean {
    if (!isReplayable(message)) {
      return false
    }

    val now = clock.now()
    dropExpired(now)

    if (collapseInPlace(message, now)) {
      return true
    }

    pending.addLast(Pending(message, now))
    while (pending.size > capacity) {
      val dropped = pending.removeFirst()
      Timber.d("Pending command buffer full, dropping ${dropped.message}")
      discardedSinceNotify++
    }
    return true
  }

  @Synchronized
  private fun drainLocked(): List<SocketMessage> {
    dropExpired(clock.now())
    val replayable = pending.map { it.message }
    pending.clear()
    return replayable
  }

  /**
   * Replaces an earlier command for the same value-setting context, keeping its position in the
   * queue and its original age. Returns false when [message] is not a collapsible command, or when
   * there is nothing to collapse it into.
   */
  private fun collapseInPlace(message: SocketMessage, now: Long): Boolean {
    if (message.context !in LAST_WRITE_WINS) {
      return false
    }
    val index = pending.indexOfFirst { it.message.context == message.context }
    if (index == -1) {
      return false
    }
    // A command that keeps failing must age from when the user issued it, not from the last retry,
    // otherwise it can be replayed indefinitely on a flapping connection.
    val queuedAt = if (pending[index].message == message) pending[index].queuedAt else now
    pending[index] = Pending(message, queuedAt)
    return true
  }

  private fun dropExpired(now: Long) {
    pending.removeAll { entry ->
      val expired = now - entry.queuedAt >= ttlMs
      if (expired) {
        Timber.d("Dropping stale pending command ${entry.message}")
        discardedSinceNotify++
      }
      expired
    }
  }

  /**
   * Hands the discard count to [onDiscarded], outside the lock so the callback cannot deadlock
   * against the buffer. Coalescing losses into a single notification is the receiver's job: the
   * buffer only reports what it gave up on.
   */
  private fun reportDiscarded() {
    val count = takeDiscardCount()
    if (count > 0) {
      onDiscarded(count)
    }
  }

  @Synchronized
  private fun takeDiscardCount(): Int {
    val count = discardedSinceNotify
    discardedSinceNotify = 0
    return count
  }

  private fun isReplayable(message: SocketMessage): Boolean = message.context in REPLAYABLE

  companion object {
    /**
     * A dropped command is worth replaying only while the user still expects it to happen, but the
     * window has to outlast a reconnect or nothing is ever replayed.
     *
     * The retry driver waits 5s before its first attempt and widens the gap by 5s each time, so
     * this covers roughly the first three cycles. That is deliberately short of the driver's full
     * budget: a reconnect that succeeds does so early, and a command the user issued a minute ago
     * is no longer one they are waiting on.
     */
    const val DEFAULT_TTL_MS = 45_000L
    const val DEFAULT_CAPACITY = 16

    /**
     * Commands that still mean the same thing on a later connection.
     *
     * Deliberately an allow-list. Replay is only safe for commands addressed at the player itself,
     * so anything scoped to a position or to whatever happens to be playing is left out: replaying
     * a queue removal or a rating up to [DEFAULT_TTL_MS] later would hit whatever moved into that
     * slot in the meantime. A deny-list would also make every protocol context added in the future
     * replayable by default, with nothing forcing that decision to be made.
     */
    private val REPLAYABLE = setOf(
      Protocol.PlayerVolume.context,
      Protocol.PlayerMute.context,
      Protocol.PlayerRepeat.context,
      Protocol.PlayerShuffle.context,
      Protocol.PlayerState.context,
      Protocol.PlayerPlayPause.context,
      Protocol.PlayerPlay.context,
      Protocol.PlayerPause.context,
      Protocol.PlayerStop.context,
      Protocol.PlayerNext.context,
      Protocol.PlayerPrevious.context
    )

    /**
     * Commands that set a value rather than describe an event: only the newest matters. A subset of
     * [REPLAYABLE].
     *
     * `playpause` is here despite being a toggle. Offline the player never confirms anything, so
     * the UI does not move and a user jabbing the button three times is asking for one thing, not
     * for three toggles.
     */
    private val LAST_WRITE_WINS = setOf(
      Protocol.PlayerVolume.context,
      Protocol.PlayerMute.context,
      Protocol.PlayerRepeat.context,
      Protocol.PlayerShuffle.context,
      Protocol.PlayerState.context,
      Protocol.PlayerPlayPause.context
    )
  }
}
