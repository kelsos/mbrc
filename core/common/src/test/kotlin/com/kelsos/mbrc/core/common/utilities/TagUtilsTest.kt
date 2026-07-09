package com.kelsos.mbrc.core.common.utilities

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TagUtilsTest {
  @Test
  fun `single value returns itself`() {
    assertThat(splitTags("Rock")).containsExactly("Rock")
  }

  @Test
  fun `splits on semicolon with trailing space (genre style)`() {
    assertThat(splitTags("Gothic Metal; Power Metal; Metal"))
      .containsExactly("Gothic Metal", "Power Metal", "Metal")
      .inOrder()
  }

  @Test
  fun `splits on semicolon without space (artist style)`() {
    assertThat(splitTags("Leaves' Eyes;Metalium"))
      .containsExactly("Leaves' Eyes", "Metalium")
      .inOrder()
  }

  @Test
  fun `trims ragged whitespace around values`() {
    assertThat(splitTags("A ;  B ;C")).containsExactly("A", "B", "C").inOrder()
  }

  @Test
  fun `drops empty fragments`() {
    assertThat(splitTags("A;;B;")).containsExactly("A", "B").inOrder()
  }

  @Test
  fun `drops duplicates keeping first occurrence`() {
    assertThat(splitTags("Rock; Metal; Rock")).containsExactly("Rock", "Metal").inOrder()
  }

  @Test
  fun `blank input yields empty list`() {
    assertThat(splitTags("   ")).isEmpty()
    assertThat(splitTags("")).isEmpty()
  }

  @Test
  fun `folding matches ascii case the way NOCASE does`() {
    assertThat(foldForTagIndex("Power Metal")).isEqualTo(foldForTagIndex("power metal"))
    assertThat(foldForTagIndex("KELDIAN")).isEqualTo(foldForTagIndex("Keldian"))
  }

  /**
   * SQLite's NOCASE folds ASCII only, so anything beyond it stays distinct. Folding more here than
   * the database does would collapse two rows it keeps apart, and the id of one of them would be
   * lost from the lookup.
   */
  @Test
  fun `folding leaves non ascii case alone, as NOCASE does`() {
    assertThat(foldForTagIndex("Ätherisch")).isNotEqualTo(foldForTagIndex("ätherisch"))
    assertThat(foldForTagIndex("宇多田ヒカル")).isEqualTo("宇多田ヒカル")
  }
}
