package com.kelsos.mbrc.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frame timing for the screen the app spends most of its time on.
 *
 * Exists because the numbers that drove the Compose recomposition work were collected by hand with
 * `dumpsys gfxinfo` on a debug build, against a live connection. Three consecutive runs of an
 * identical binary came back between 27% and 100% janky frames, because a reconnect spinner was
 * cycling through some windows and not others. Nothing in that spread is a regression signal.
 *
 * This measures a release-compiled build driving a fixed gesture, so a change in the numbers is a
 * change in the app.
 *
 * ```
 * ./gradlew :baselineprofile:connectedGithubBenchmarkReleaseAndroidTest
 * ```
 *
 * A device the harness cannot leave idle will still produce noise: the queue is only worth
 * measuring with tracks in it, so the plugin has to have been reachable at least once, and the
 * screen has to stay awake for the run (Settings has a "Keep screen on" toggle for exactly this).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class QueueScrollBenchmark {

  @get:Rule
  val rule = MacrobenchmarkRule()

  @Test
  fun scrollQueueCompilationNone() = scrollQueue(CompilationMode.None())

  @Test
  fun scrollQueueCompilationBaselineProfile() = scrollQueue(CompilationMode.Partial())

  private fun scrollQueue(compilationMode: CompilationMode) = rule.measureRepeated(
    packageName = PACKAGE_NAME,
    metrics = listOf(FrameTimingMetric(), StartupTimingMetric()),
    compilationMode = compilationMode,
    startupMode = StartupMode.COLD,
    iterations = ITERATIONS,
    setupBlock = {
      pressHome()
      grantLocalNetworkAccess()
    }
  ) {
    startActivityAndWait()
    dismissWhatsNew()
    openDestination(QUEUE)
    waitForContent()
    scrollCurrentList()
  }

  private companion object {
    const val ITERATIONS = 10
  }
}
