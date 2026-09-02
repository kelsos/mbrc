package com.kelsos.mbrc.core.common.state

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrackDetailsTest {

  @Test
  fun `an untouched instance reports no data`() {
    assertThat(TrackDetails().hasData()).isFalse()
    assertThat(TrackDetails.EMPTY.hasData()).isFalse()
  }

  @Test
  fun `a single populated field is enough to count as data`() {
    assertThat(TrackDetails(composer = "a composer").hasData()).isTrue()
  }

  @Test
  fun `track number is paired with its total when both are known`() {
    val details = TrackDetails(trackNo = "3", trackCount = "12")

    assertThat(details.formatTrackNumber()).isEqualTo("3/12")
  }

  @Test
  fun `track number stands alone when the total is missing`() {
    assertThat(TrackDetails(trackNo = "3").formatTrackNumber()).isEqualTo("3")
    assertThat(TrackDetails(trackNo = "3", trackCount = "  ").formatTrackNumber()).isEqualTo("3")
  }

  @Test
  fun `a missing track number formats to nothing, total or not`() {
    assertThat(TrackDetails().formatTrackNumber()).isEmpty()
    assertThat(TrackDetails(trackCount = "12").formatTrackNumber()).isEmpty()
    assertThat(TrackDetails(trackNo = "  ", trackCount = "12").formatTrackNumber()).isEmpty()
  }

  @Test
  fun `disc number follows the same rules as track number`() {
    assertThat(TrackDetails(discNo = "1", discCount = "2").formatDiscNumber()).isEqualTo("1/2")
    assertThat(TrackDetails(discNo = "1").formatDiscNumber()).isEqualTo("1")
    assertThat(TrackDetails(discCount = "2").formatDiscNumber()).isEmpty()
    assertThat(TrackDetails().formatDiscNumber()).isEmpty()
  }
}
