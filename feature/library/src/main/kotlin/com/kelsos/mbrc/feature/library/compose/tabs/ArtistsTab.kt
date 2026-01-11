package com.kelsos.mbrc.feature.library.compose.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.kelsos.mbrc.core.common.settings.ArtistSortField
import com.kelsos.mbrc.core.common.settings.ArtistSortPreference
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.common.settings.SortPreference
import com.kelsos.mbrc.core.common.utilities.AppError
import com.kelsos.mbrc.core.common.utilities.Outcome
import com.kelsos.mbrc.core.data.library.artist.Artist
import com.kelsos.mbrc.core.queue.Queue
import com.kelsos.mbrc.feature.library.R
import com.kelsos.mbrc.feature.library.artists.ArtistUiMessage
import com.kelsos.mbrc.feature.library.artists.BrowseArtistViewModel
import com.kelsos.mbrc.feature.library.compose.SortBottomSheet
import com.kelsos.mbrc.feature.library.compose.SortOption
import com.kelsos.mbrc.feature.library.compose.components.ArtistListItem
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel

private val artistSortOptions = listOf(
  SortOption(ArtistSortField.NAME, R.string.sort_by_name)
)

@Composable
fun ArtistsTab(
  snackbarHostState: SnackbarHostState,
  isSyncing: Boolean,
  onNavigateToArtistAlbums: (Artist) -> Unit,
  onSync: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: BrowseArtistViewModel = koinViewModel()
) {
  val artists = viewModel.artists.collectAsLazyPagingItems()
  val showSync by viewModel.showSync.collectAsState(initial = true)
  val sortPreference by viewModel.sortPreference.collectAsState(
    initial = SortPreference(ArtistSortField.NAME, SortOrder.ASC)
  )
  var showSortSheet by rememberSaveable { mutableStateOf(false) }

  // Handle navigation events
  LaunchedEffect(Unit) {
    viewModel.events.filterIsInstance<ArtistUiMessage.OpenArtistAlbums>().collect { event ->
      onNavigateToArtistAlbums(event.artist)
    }
  }

  // Handle queue results
  val queueResults = remember {
    viewModel.events.map { event ->
      when (event) {
        is ArtistUiMessage.QueueSuccess -> Outcome.Success(event.tracksCount)
        is ArtistUiMessage.QueueFailed -> Outcome.Failure(AppError.OperationFailed)
        is ArtistUiMessage.NetworkUnavailable -> Outcome.Failure(AppError.NetworkUnavailable)
        else -> null
      }
    }.filterIsInstance<Outcome<Int>>()
  }

  Box(modifier = modifier) {
    LibraryBrowseTab(
      items = artists,
      queueResults = queueResults,
      snackbarHostState = snackbarHostState,
      syncState = SyncState(
        isSyncing = isSyncing,
        showSync = showSync,
        onSync = onSync
      ),
      emptyState = EmptyState(
        message = stringResource(R.string.artists_list_empty),
        icon = Icons.Default.Person
      ),
      itemKey = { it.id }
    ) { artist ->
      ArtistListItem(
        artist = artist,
        onClick = { viewModel.queue(Queue.Default, artist) },
        onQueue = { queue -> viewModel.queue(queue, artist) }
      )
    }

    FloatingActionButton(
      onClick = { showSortSheet = true },
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp),
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.Sort,
        contentDescription = stringResource(R.string.sort_button_description)
      )
    }
  }

  if (showSortSheet) {
    SortBottomSheet(
      title = stringResource(R.string.sort_title),
      options = artistSortOptions,
      selectedField = sortPreference.field,
      selectedOrder = sortPreference.order,
      onSortSelected = { field, order ->
        viewModel.updateSortPreference(ArtistSortPreference(field, order))
      },
      onDismiss = { showSortSheet = false }
    )
  }
}
