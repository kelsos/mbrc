package com.kelsos.mbrc.features.library

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kelsos.mbrc.BaseActivity
import com.kelsos.mbrc.R
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class LibraryActivity :
  BaseActivity(R.layout.activity_library),
  OnQueryTextListener {
  private lateinit var pager: ViewPager2
  private lateinit var tabs: TabLayout

  private var searchView: SearchView? = null
  private var searchMenuItem: MenuItem? = null
  private var albumArtistOnly: MenuItem? = null
  private var searchClear: MenuItem? = null
  private var pagerAdapter: LibraryPagerAdapter? = null

  private val viewModel: LibraryViewModel by inject()

  override fun onQueryTextSubmit(query: String): Boolean {
    val search = query.trim()
    if (search.isNotEmpty()) {
      closeSearch()
      viewModel.search(search)
      supportActionBar?.title = search
      supportActionBar?.setSubtitle(R.string.library_search_subtitle)
      searchMenuItem?.isVisible = false
      searchClear?.isVisible = true
    } else {
      viewModel.search()
    }

    return true
  }

  internal fun closeSearch(): Boolean {
    searchView?.let {
      if (it.isShown) {
        it.isIconified = true
        it.isFocusable = false
        it.clearFocus()
        searchMenuItem?.collapseActionView()
        return true
      }
    }
    return false
  }

  override fun onQueryTextChange(newText: String): Boolean = false

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (closeSearch()) {
            return
          }
          onBackPressedDispatcher.onBackPressed()
        }
      },
    )

    pager = findViewById(R.id.search_pager)
    tabs = findViewById(R.id.pager_tab_strip)
    pagerAdapter = LibraryPagerAdapter(this)
    pager.adapter = pagerAdapter

    TabLayoutMediator(tabs, pager) { currentTab, currentPosition ->
      currentTab.text =
        when (currentPosition) {
          PagePosition.ALBUMS -> getString(R.string.media__albums)
          PagePosition.ARTISTS -> getString(R.string.media__artists)
          PagePosition.GENRES -> getString(R.string.media__genres)
          PagePosition.TRACKS -> getString(R.string.media__tracks)
          else -> throw IllegalArgumentException("invalid position")
        }
    }.attach()

    observeViewModel()
  }

  fun observeViewModel() {
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
          when (event) {
            is LibraryUiEvent.LibraryStatsReady -> showStats(event.stats)
            is LibraryUiEvent.LibrarySyncComplete -> {
              syncComplete(event.stats)
              hideSyncProgress()
            }

            is LibraryUiEvent.UpdateAlbumArtistOnly -> updateArtistOnlyPreference(event.enabled)
            is LibraryUiEvent.LibrarySyncFailed -> {
              syncFailure()
              hideSyncProgress()
            }
          }
        }
      }
    }

    lifecycleScope.launch {
      viewModel.progress.collect { progress ->
        Timber.v("progress: $progress")
        if (progress.running) {
          showSyncProgress(progress)
        } else {
          hideSyncProgress()
        }
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.library_search, menu)
    searchMenuItem = menu.findItem(R.id.library_search_item)
    searchClear = menu.findItem(R.id.library_search_clear)
    albumArtistOnly = menu.findItem(R.id.library_album_artist)
    searchView = searchMenuItem?.actionView as SearchView

    val search = requireNotNull(searchView)

    search.queryHint = getString(R.string.library_search_hint)
    search.setIconifiedByDefault(true)
    search.setOnQueryTextListener(this)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean =
    when (item.itemId) {
      R.id.library_refresh_item -> {
        viewModel.sync()
        true
      }

      R.id.library_album_artist -> {
        albumArtistOnly?.let {
          it.isChecked = !it.isChecked
          viewModel.updateAlbumArtistOnly(it.isChecked)
        }
        true
      }

      R.id.library_search_clear -> {
        supportActionBar?.setTitle(R.string.nav_library)
        supportActionBar?.subtitle = ""
        viewModel.search()
        searchMenuItem?.isVisible = true
        searchClear?.isVisible = false
        true
      }

      R.id.library_sync_state -> {
        viewModel.displayLibraryStats()
        true
      }

      else -> super.onOptionsItemSelected(item)
    }

  fun showStats(stats: LibraryStats) {
    val dialog =
      MaterialAlertDialogBuilder(this)
        .setTitle(R.string.library_stats__title)
        .setView(R.layout.library_stats__layout)
        .setPositiveButton(android.R.string.ok) { md, _ -> md.dismiss() }
        .show()

    dialog.findViewById<TextView>(R.id.library_stats__genre_value)?.text = "${stats.genres}"
    dialog.findViewById<TextView>(R.id.library_stats__artist_value)?.text = "${stats.artists}"
    dialog.findViewById<TextView>(R.id.library_stats__album_value)?.text = "${stats.albums}"
    dialog.findViewById<TextView>(R.id.library_stats__track_value)?.text = "${stats.tracks}"
    dialog.findViewById<TextView>(R.id.library_stats__playlist_value)?.text = "${stats.playlists}"
  }

  fun syncComplete(stats: LibraryStats) {
    val message =
      getString(
        R.string.library__sync_complete,
        resources.getQuantityString(R.plurals.genre, stats.genres.toInt(), stats.genres.toInt()),
        resources.getQuantityString(R.plurals.artist, stats.artists.toInt(), stats.artists.toInt()),
        resources.getQuantityString(R.plurals.album, stats.albums.toInt(), stats.albums.toInt()),
        resources.getQuantityString(R.plurals.track, stats.tracks.toInt(), stats.tracks.toInt()),
        resources.getQuantityString(R.plurals.playlist, stats.playlists.toInt(), stats.playlists.toInt()),
      )
    Snackbar
      .make(pager, R.string.library__sync_complete_title, Snackbar.LENGTH_LONG)
      .setText(message)
      .show()
  }

  public override fun onDestroy() {
    super.onDestroy()
    pagerAdapter = null
  }

  fun updateArtistOnlyPreference(albumArtistOnly: Boolean?) {
    this.albumArtistOnly?.isChecked = albumArtistOnly == true
  }

  override fun active(): Int = R.id.nav_library

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt(PAGER_POSITION, pager.currentItem)
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    pager.currentItem = savedInstanceState.getInt(PAGER_POSITION, 0)
  }

  fun syncFailure() {
    Snackbar.make(pager, R.string.library__sync_failed, Snackbar.LENGTH_LONG).show()
  }

  fun getCategoryText(mediaType: LibraryMediaType): String =
    when (mediaType) {
      LibraryMediaType.Albums -> getString(R.string.media__albums)
      LibraryMediaType.Artists -> getString(R.string.media__artists)
      LibraryMediaType.Genres -> getString(R.string.media__genres)
      LibraryMediaType.Playlists -> getString(R.string.media__playlists)
      LibraryMediaType.Tracks -> getString(R.string.media__tracks)
      LibraryMediaType.Covers -> getString(R.string.media__covers)
    }

  fun showSyncProgress(progress: LibrarySyncProgress) {
    val syncText = findViewById<TextView>(R.id.sync_progress_text)
    val progressIndicator = findViewById<LinearProgressIndicator>(R.id.sync_progress)
    val categoryText = getCategoryText(progress.category)
    syncText.isGone = false
    syncText.text = getString(R.string.library__sync_progress, progress.current, progress.total, categoryText)
    progressIndicator.isGone = false
    progressIndicator.progress = progress.current
    progressIndicator.max = progress.total
  }

  fun hideSyncProgress() {
    findViewById<LinearProgressIndicator>(R.id.sync_progress).isGone = true
    findViewById<TextView>(R.id.sync_progress_text).isGone = true
  }

  companion object {
    private const val PAGER_POSITION = "com.kelsos.mbrc.ui.activities.nav.PAGER_POSITION"
  }
}
