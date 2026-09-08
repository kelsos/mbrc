package com.kelsos.mbrc.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

/**
 * The benchmark builds share the shipping application id, so a store build of MusicBee Remote has
 * to be uninstalled from the device first. Its signature will not match, and the run fails on
 * INSTALL_FAILED_UPDATE_INCOMPATIBLE without saying why.
 *
 * Giving these builds a suffixed id looks like the fix and is not: the baseline profile plugin
 * creates its build types with initWith(release), which copies applicationIdSuffix back over
 * anything configured here.
 */
internal const val PACKAGE_NAME = "com.kelsos.mbrc"

/** Where the app opens. Every list is somewhere else, so a journey has to navigate to reach one. */
internal const val NOW_PLAYING = "Now playing"
internal const val QUEUE = "Queue"
internal const val LIBRARY = "Library"

/**
 * The library's tabs, each a paging source and a row of its own, so one scroll of whichever opens
 * first leaves the other three uncompiled. Albums is a grid rather than a list.
 */
internal val LIBRARY_TABS = listOf("Genres", "Artists", "Albums", "Tracks")

private const val DRAWER_BUTTON = "Open navigation drawer"

/** The drawer's own header. Destination labels double as screen titles, so they cannot be used. */
private const val DRAWER_HEADER = "MusicBee Remote"

/** The What's New overlay's dismiss button. */
private const val WHATS_NEW_DISMISS = "Got it"

/** The library's own sync action, preferred over emulating a pull wherever it exists. */
private const val SYNC_ACTION = "Sync library"

/** Short: this is paid on every iteration, and most of them will not show an overlay at all. */
private const val OVERLAY_WAIT_MILLIS = 2_000L
private const val WAIT_MILLIS = 5_000L
private const val SCROLL_PERCENT = 0.8f
private const val GESTURE_MARGIN_FRACTION = 5

/**
 * How far down the list is pulled, and how fast.
 *
 * The speed is the part that matters: at UiAutomator's default the gesture is read as a fling and
 * springs back, and roughly this is what a thumb does.
 */
private const val PULL_PERCENT = 0.8f
private const val PULL_SPEED_PIXELS_PER_SECOND = 1_500

/** The progress bar's text, shown by both the queue and the library while a sync runs. */
private const val SYNC_PROGRESS = "Syncing"
private const val REFRESH_START_MILLIS = 3_000L

/** Discovery, connection and the first data push all have to land inside this. */
private const val CONNECT_TIMEOUT_MILLIS = 30_000L

/**
 * Long enough for a real library to finish.
 *
 * A sync that outlives this leaves the journey walking on while it runs, and the next iteration
 * kills the app in the middle of it. Only the first iteration refreshes, so the wait is paid once
 * however generous it is.
 */
private const val SYNC_TIMEOUT_MILLIS = 300_000L

/**
 * Grants local network access up front.
 *
 * Everything else here depends on it: the app discovers the plugin over the local network and
 * connects to it on its own, which is where the queue and the library come from. Android 17 gates
 * that behind a runtime permission, and the activity explains it with a dialog before the system
 * prompt. The dialog's "already declined" flag is instance state, so without the grant it returns
 * on every launch of a run, over the top of the journey.
 *
 * Older releases have no such permission and fail the command harmlessly.
 */
internal fun MacrobenchmarkScope.grantLocalNetworkAccess() {
  runCatching {
    device.executeShellCommand("pm grant $PACKAGE_NAME android.permission.ACCESS_LOCAL_NETWORK")
  }
}

/**
 * Waits for a list with something in it.
 *
 * A fresh install starts empty and fills once discovery finds the plugin and the connection
 * settles, so a scroll issued before that scrolls nothing and records nothing.
 */
internal fun MacrobenchmarkScope.waitForContent() {
  device.wait(Until.hasObject(By.scrollable(true)), CONNECT_TIMEOUT_MILLIS)
  device.waitForIdle()
}

/**
 * Pulls the list down far enough to trigger a refresh, and lets the sync that follows settle.
 *
 * Worth recording: a refresh is how the library and the queue get their content, so it pulls in
 * the repository and protocol paths that a scroll alone never reaches. Deliberately absent from
 * the frame timing benchmark, where a sync landing mid-measurement is noise.
 */
internal fun MacrobenchmarkScope.pullToRefresh() {
  // The library carries a sync action, and a button press is a gesture that cannot half-happen.
  // The queue has no such action, so there the pull is the only way in.
  val syncAction = device.findObject(By.desc(SYNC_ACTION))
  if (syncAction != null) {
    runCatching { syncAction.click() }
  } else {
    // Swiped on the list itself at an explicit speed rather than dragged across the screen. A
    // drag moves as fast as the steps allow, and the pull is then read as a fling and springs
    // back without refreshing: done by hand at human speed the same gesture works.
    device.wait(Until.hasObject(By.scrollable(true)), WAIT_MILLIS)
    val list = device.findObject(By.scrollable(true))
    runCatching {
      list?.setGestureMargin(device.displayWidth / GESTURE_MARGIN_FRACTION)
      list?.swipe(Direction.DOWN, PULL_PERCENT, PULL_SPEED_PIXELS_PER_SECOND)
    }
  }

  // A sync is the point of the pull, so wait for it to finish rather than for a fixed delay:
  // scrolling a list whose pages have not arrived records an empty list. The progress bar carries
  // the only text either screen shows while it runs.
  device.wait(Until.hasObject(By.textStartsWith(SYNC_PROGRESS)), REFRESH_START_MILLIS)
  device.wait(Until.gone(By.textStartsWith(SYNC_PROGRESS)), SYNC_TIMEOUT_MILLIS)
  device.waitForIdle()
}

/**
 * Closes the What's New overlay.
 *
 * A fresh install shows it once, so the first iteration of a run always gets it and the ones after
 * do not: the app records the version as seen when it shows it. Left up it covers the screen and
 * swallows the navigation, so the journey would record the changelog instead of the app.
 */
internal fun MacrobenchmarkScope.dismissWhatsNew() {
  device.wait(Until.hasObject(By.text(WHATS_NEW_DISMISS)), OVERLAY_WAIT_MILLIS)
  runCatching { device.findObject(By.text(WHATS_NEW_DISMISS))?.click() }
  device.waitForIdle()
}

/** Opens a library tab by its label, if the tab row is on screen. */
internal fun MacrobenchmarkScope.openTab(label: String) {
  device.wait(Until.hasObject(By.text(label)), WAIT_MILLIS)
  runCatching { device.findObject(By.text(label))?.click() }
  device.waitForIdle()
}

/**
 * Scrolls whatever list is on screen, or returns if the screen has none.
 *
 * The scrollable is looked up again for every scroll rather than held across them. Paging swaps
 * the list out as pages load and the library's pager swaps it out on a tab change, either of which
 * leaves a held handle stale, and a StaleObjectException fails the whole run.
 */
internal fun MacrobenchmarkScope.scrollCurrentList(times: Int = 3) {
  repeat(times) {
    device.wait(Until.hasObject(By.scrollable(true)), WAIT_MILLIS)
    val list = device.findObject(By.scrollable(true)) ?: return
    runCatching {
      list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_FRACTION)
      list.scroll(Direction.DOWN, SCROLL_PERCENT)
    }
    device.waitForIdle()
  }
}

/**
 * Navigates through the drawer or the rail, whichever the width class put on screen.
 *
 * The drawer has to be opened first; the rail is already there and has no button, so a missing
 * hamburger is expected rather than a failure.
 *
 * Both ends are waited on against the drawer's header, which is the one piece of it that is not
 * also a screen title. Tapping the hamburger toggles, so a tap arriving while the drawer is still
 * closing from the previous destination reopens it and leaves the journey stuck behind it, and
 * waitForIdle returns too early to prevent that on its own.
 */
internal fun MacrobenchmarkScope.openDestination(label: String) {
  runCatching {
    if (!drawerIsOpen()) {
      device.findObject(By.desc(DRAWER_BUTTON))?.click()
      device.wait(Until.hasObject(By.text(label)), WAIT_MILLIS)
    }
    device.findObject(By.text(label))?.click()
  }
  device.wait(Until.gone(By.text(DRAWER_HEADER)), WAIT_MILLIS)
  device.waitForIdle()
}

private fun MacrobenchmarkScope.drawerIsOpen() = device.hasObject(By.text(DRAWER_HEADER))
