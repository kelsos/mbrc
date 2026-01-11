package com.kelsos.mbrc.feature.library.compose.drilldown

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Album
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
import com.kelsos.mbrc.core.common.settings.AlbumSortField
import com.kelsos.mbrc.core.common.settings.AlbumSortPreference
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.common.settings.SortPreference
import com.kelsos.mbrc.core.common.utilities.AppError
import com.kelsos.mbrc.core.common.utilities.Outcome
import com.kelsos.mbrc.core.data.library.album.Album
import com.kelsos.mbrc.core.queue.Queue
import com.kelsos.mbrc.core.ui.compose.NavigationIconType
import com.kelsos.mbrc.core.ui.compose.PagingListScreen
import com.kelsos.mbrc.core.ui.compose.QueueResultEffect
import com.kelsos.mbrc.core.ui.compose.ScreenScaffold
import com.kelsos.mbrc.feature.library.R
import com.kelsos.mbrc.feature.library.albums.AlbumUiMessage
import com.kelsos.mbrc.feature.library.albums.ArtistAlbumsViewModel
import com.kelsos.mbrc.feature.library.compose.SortBottomSheet
import com.kelsos.mbrc.feature.library.compose.SortOption
import com.kelsos.mbrc.feature.library.compose.components.AlbumListItem
import com.kelsos.mbrc.feature.minicontrol.MiniControl
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel

private val albumSortOptions = listOf(
  SortOption(AlbumSortField.NAME, R.string.sort_by_name),
  SortOption(AlbumSortField.YEAR, R.string.sort_by_year)
)

@Composable
fun ArtistAlbumsScreen(
  artistName: String,
  onNavigateBack: () -> Unit,
  onNavigateToAlbumTracks: (Album) -> Unit,
  onNavigateToPlayer: () -> Unit,
  snackbarHostState: SnackbarHostState,
  modifier: Modifier = Modifier,
  viewModel: ArtistAlbumsViewModel = koinViewModel()
) {
  val albums = viewModel.albums.collectAsLazyPagingItems()
  val sortPreference by viewModel.sortPreference.collectAsState(
    initial = SortPreference(AlbumSortField.YEAR, SortOrder.ASC)
  )
  var showSortSheet by rememberSaveable { mutableStateOf(false) }

  // Load artist albums
  LaunchedEffect(artistName) {
    viewModel.load(artistName)
  }

  LaunchedEffect(Unit) {
    viewModel.events.filterIsInstance<AlbumUiMessage.OpenAlbumTracks>().collect { event ->
      onNavigateToAlbumTracks(event.album)
    }
  }

  val queueResults = remember {
    viewModel.events.map { event ->
      when (event) {
        is AlbumUiMessage.QueueSuccess -> Outcome.Success(event.tracksCount)
        is AlbumUiMessage.QueueFailed -> Outcome.Failure(AppError.OperationFailed)
        is AlbumUiMessage.NetworkUnavailable -> Outcome.Failure(AppError.NetworkUnavailable)
        else -> null
      }
    }.filterIsInstance<Outcome<Int>>()
  }

  QueueResultEffect(
    queueResults = queueResults,
    snackbarHostState = snackbarHostState
  )

  ScreenScaffold(
    title = artistName,
    snackbarHostState = snackbarHostState,
    navigationIcon = NavigationIconType.Back(onNavigateBack),
    modifier = modifier
  ) { paddingValues ->
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
      Box(modifier = Modifier.weight(1f)) {
        PagingListScreen(
          items = albums,
          modifier = Modifier.fillMaxSize(),
          emptyMessage = stringResource(R.string.albums_list_empty),
          emptyIcon = Icons.Default.Album,
          key = { it.id }
        ) { album ->
          AlbumListItem(
            album = album,
            onClick = { viewModel.queue(Queue.Default, album) },
            onQueue = { queue -> viewModel.queue(queue, album) }
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

      MiniControl(
        onNavigateToPlayer = onNavigateToPlayer,
        snackbarHostState = snackbarHostState
      )
    }
  }

  if (showSortSheet) {
    SortBottomSheet(
      title = stringResource(R.string.sort_title),
      options = albumSortOptions,
      selectedField = sortPreference.field,
      selectedOrder = sortPreference.order,
      onSortSelected = { field, order ->
        viewModel.updateSortPreference(AlbumSortPreference(field, order))
      },
      onDismiss = { showSortSheet = false }
    )
  }
}
