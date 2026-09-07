package com.kelsos.mbrc.core.common.layout

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WindowWidthClassTest {
  @Test
  fun `width classes split at the material breakpoints`() {
    assertThat(WindowWidthClass.fromWidthDp(599)).isEqualTo(WindowWidthClass.Compact)
    assertThat(WindowWidthClass.fromWidthDp(600)).isEqualTo(WindowWidthClass.Medium)
    assertThat(WindowWidthClass.fromWidthDp(839)).isEqualTo(WindowWidthClass.Medium)
    assertThat(WindowWidthClass.fromWidthDp(840)).isEqualTo(WindowWidthClass.Expanded)
  }

  @Test
  fun `a phone in landscape is not treated as a tablet`() {
    // 720x360: wider than it is tall, but only medium - it must not get the two-pane treatment
    // that an expanded window gets, which is what branching on orientation used to do.
    val phoneLandscape = WindowWidthClass.fromWidthDp(720)

    assertThat(phoneLandscape.supportsTwoPanes).isFalse()
    assertThat(phoneLandscape.prefersPersistentNavigation).isTrue()
  }

  @Test
  fun `only compact keeps navigation behind a menu button`() {
    assertThat(WindowWidthClass.Compact.prefersPersistentNavigation).isFalse()
    assertThat(WindowWidthClass.Medium.prefersPersistentNavigation).isTrue()
    assertThat(WindowWidthClass.Expanded.prefersPersistentNavigation).isTrue()
  }

  @Test
  fun `only expanded gets two panes`() {
    assertThat(WindowWidthClass.Compact.supportsTwoPanes).isFalse()
    assertThat(WindowWidthClass.Medium.supportsTwoPanes).isFalse()
    assertThat(WindowWidthClass.Expanded.supportsTwoPanes).isTrue()
  }
}
