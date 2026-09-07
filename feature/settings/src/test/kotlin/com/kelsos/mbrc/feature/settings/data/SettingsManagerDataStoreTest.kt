package com.kelsos.mbrc.feature.settings.data

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.core.common.settings.AlbumSortField
import com.kelsos.mbrc.core.common.settings.AlbumViewMode
import com.kelsos.mbrc.core.common.settings.ArtistSortField
import com.kelsos.mbrc.core.common.settings.GenreSortField
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.common.settings.SortPreference
import com.kelsos.mbrc.core.common.settings.TrackSortField
import com.kelsos.mbrc.core.common.test.testDispatchers
import com.kelsos.mbrc.core.common.utilities.AppInfo
import com.kelsos.mbrc.feature.settings.data.SettingsDataStore.dataStore
import com.kelsos.mbrc.feature.settings.domain.SettingsManager
import com.kelsos.mbrc.feature.settings.theme.Theme
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The data store is a process singleton keyed by the application context, so every test starts by
 * clearing it: without that, a version written by one test would decide the outcome of the next.
 */
@RunWith(AndroidJUnit4::class)
class SettingsManagerDataStoreTest {

  private val context: Application = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    runBlocking { context.dataStore.edit { it.clear() } }
  }

  @Test
  fun `the changelog is shown on a fresh install`() = runTest {
    val manager = manager(versionCode = 130)

    assertThat(manager.checkShouldShowChangeLog()).isTrue()
  }

  @Test
  fun `the changelog is shown once per version`() = runTest {
    val manager = manager(versionCode = 130)

    manager.checkShouldShowChangeLog()

    assertWithMessage("the version was recorded by the first check")
      .that(manager.checkShouldShowChangeLog())
      .isFalse()
  }

  @Test
  fun `the changelog is shown again after an update`() = runTest {
    manager(versionCode = 130).checkShouldShowChangeLog()

    assertThat(manager(versionCode = 131).checkShouldShowChangeLog()).isTrue()
  }

  @Test
  fun `a downgrade does not show the changelog`() = runTest {
    manager(versionCode = 131).checkShouldShowChangeLog()

    assertThat(manager(versionCode = 130).checkShouldShowChangeLog()).isFalse()
  }

  @Test
  fun `an update check that never ran reads as the epoch`() = runTest {
    val manager = manager()

    assertThat(manager.getLastUpdated(required = false)).isEqualTo(Instant.EPOCH)
    assertThat(manager.getLastUpdated(required = true)).isEqualTo(Instant.EPOCH)
  }

  @Test
  fun `the required and optional update checks are stored separately`() = runTest {
    val manager = manager()
    val optional = Instant.ofEpochMilli(1_000)
    val required = Instant.ofEpochMilli(2_000)

    manager.setLastUpdated(optional, required = false)
    manager.setLastUpdated(required, required = true)

    assertThat(manager.getLastUpdated(required = false)).isEqualTo(optional)
    assertThat(manager.getLastUpdated(required = true)).isEqualTo(required)
  }

  @Test
  fun `writing the optional check leaves the required one untouched`() = runTest {
    val manager = manager()
    val required = Instant.ofEpochMilli(2_000)

    manager.setLastUpdated(required, required = true)
    manager.setLastUpdated(Instant.ofEpochMilli(5_000), required = false)

    assertThat(manager.getLastUpdated(required = true)).isEqualTo(required)
  }

  @Test
  fun `every sort preference round trips through its own key`() = runTest {
    val manager = manager()

    manager.setGenreSortPreference(SortPreference(GenreSortField.NAME, SortOrder.DESC))
    manager.setArtistSortPreference(SortPreference(ArtistSortField.NAME, SortOrder.DESC))
    manager.setAlbumSortPreference(SortPreference(AlbumSortField.ARTIST, SortOrder.DESC))
    manager.setTrackSortPreference(SortPreference(TrackSortField.ALBUM_ARTIST, SortOrder.DESC))

    assertThat(manager.genreSortPreferenceFlow.first())
      .isEqualTo(SortPreference(GenreSortField.NAME, SortOrder.DESC))
    assertThat(manager.artistSortPreferenceFlow.first())
      .isEqualTo(SortPreference(ArtistSortField.NAME, SortOrder.DESC))
    assertThat(manager.albumSortPreferenceFlow.first())
      .isEqualTo(SortPreference(AlbumSortField.ARTIST, SortOrder.DESC))
    assertThat(manager.trackSortPreferenceFlow.first())
      .isEqualTo(SortPreference(TrackSortField.ALBUM_ARTIST, SortOrder.DESC))
  }

  @Test
  fun `the artists of a genre sort apart from the artist list`() = runTest {
    val manager = manager()

    manager.setGenreArtistsSortPreference(SortPreference(ArtistSortField.NAME, SortOrder.DESC))

    assertThat(manager.genreArtistsSortPreferenceFlow.first().order).isEqualTo(SortOrder.DESC)
    assertWithMessage("the two artist sorts share a field type and are easy to cross-wire")
      .that(manager.artistSortPreferenceFlow.first().order)
      .isEqualTo(SortOrder.ASC)
  }

  @Test
  fun `the albums of an artist sort apart from the album list`() = runTest {
    val manager = manager()

    manager.setArtistAlbumsSortPreference(SortPreference(AlbumSortField.ARTIST, SortOrder.DESC))

    assertThat(manager.artistAlbumsSortPreferenceFlow.first())
      .isEqualTo(SortPreference(AlbumSortField.ARTIST, SortOrder.DESC))
    assertWithMessage("the two album sorts share a field type and are easy to cross-wire")
      .that(manager.albumSortPreferenceFlow.first())
      .isEqualTo(SortPreference(AlbumSortField.NAME, SortOrder.ASC))
  }

  @Test
  fun `an unset sort preference reads as its default rather than failing`() = runTest {
    val manager = manager()

    assertThat(manager.trackSortPreferenceFlow.first().field).isEqualTo(TrackSortField.TITLE)
    assertThat(manager.albumSortPreferenceFlow.first().field).isEqualTo(AlbumSortField.NAME)
    assertThat(manager.genreArtistsSortPreferenceFlow.first().order).isEqualTo(SortOrder.ASC)
  }

  @Test
  fun `the album view mode round trips`() = runTest {
    val manager = manager()

    manager.setAlbumViewMode(AlbumViewMode.GRID)

    assertThat(manager.albumViewModeFlow.first()).isEqualTo(AlbumViewMode.GRID)
  }

  @Test
  fun `the theme round trips`() = runTest {
    val manager = manager()

    manager.setTheme(Theme.Dark)

    assertThat(manager.themeFlow.first()).isEqualTo(Theme.Dark)
  }

  private fun manager(versionCode: Int = 130): SettingsManager = SettingsManagerDataStore(
    context = context,
    appDispatchers = testDispatchers,
    appInfo = appInfo(versionCode)
  )

  private fun appInfo(versionCode: Int): AppInfo = object : AppInfo {
    override val versionName: String = "1.7.0"
    override val versionCode: Int = versionCode
    override val buildTime: String = ""
    override val gitRevision: String = ""
    override val applicationId: String = "com.kelsos.mbrc"
  }
}
