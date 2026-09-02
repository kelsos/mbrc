package com.kelsos.mbrc.core.common.test

import org.junit.rules.Timeout

/**
 * A per-test deadline for any class driving long-lived coroutines, applied as
 * `@get:Rule val timeout: Timeout = coroutineTestTimeout()`.
 *
 * A coroutine left running when a test ends does not fail, it spins: `runTest` waits for the
 * scheduler to go idle, and a collector or `while (isActive)` loop never lets it, so the run
 * allocates until the heap thrashes. `runTest`'s own timeout cannot fire on that, because it is
 * enforced at a suspension point and the drain never suspends. Only a rule that runs the body on
 * its own thread can end it, turning a hung CI job into an ordinary failure that names the method.
 *
 * The value is deliberately far above what any test here needs, so tripping it means a bug rather
 * than a slow machine.
 *
 * That own thread is also the limit of where this can be used: anything asserting which thread it
 * is on rejects it. Media3's `Player` and Compose's test rule both do, so `RemotePlayerTest` and
 * `DragDropStateSettleTest` deliberately go without ("Player is accessed on the wrong thread").
 */
fun coroutineTestTimeout(): Timeout = Timeout.seconds(30)
