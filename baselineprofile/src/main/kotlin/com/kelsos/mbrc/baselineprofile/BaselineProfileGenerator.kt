package com.kelsos.mbrc.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a baseline profile for the app.
 *
 * Run this test on a device with API 28+ (rooted or userdebug build recommended for best results).
 *
 * To generate the profile:
 * ```
 * ./gradlew :app:generateGithubReleaseBaselineProfile
 * ```
 *
 * The generated profile will be placed in `app/src/main/baselineProfiles/`.
 *
 * The journey covers the screens the app is actually used on, not just its launch. A profile
 * recorded from a cold start alone leaves the queue, the library and the player uncompiled, which
 * is where the scrolling happens.
 *
 * The lists are seeded first. Profiles are captured against the release application id, which has
 * never reached a plugin, so without seeding every list here records its empty state.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

  @get:Rule
  val rule = BaselineProfileRule()

  @Test
  fun generateBaselineProfile() {
    rule.collect(
      packageName = PACKAGE_NAME,
      includeInStartupProfile = true
    ) {
      pressHome()
      grantLocalNetworkAccess()

      // Startup lands on the player, so every list below has to be navigated to.
      startActivityAndWait()
      dismissWhatsNew()
      device.waitForIdle()

      // Refreshed on the first iteration only. Once the sync paths are recorded the later
      // iterations gain nothing from repeating them, and against a real library each sync costs
      // minutes.
      val refreshing = iteration == 0

      openDestination(QUEUE)
      waitForContent()
      if (refreshing) {
        pullToRefresh()
      }
      scrollCurrentList()

      openDestination(LIBRARY)
      waitForContent()
      if (refreshing) {
        pullToRefresh()
      }
      LIBRARY_TABS.forEach { tab ->
        openTab(tab)
        scrollCurrentList()
      }

      openDestination(NOW_PLAYING)
      device.waitForIdle()
    }
  }
}
