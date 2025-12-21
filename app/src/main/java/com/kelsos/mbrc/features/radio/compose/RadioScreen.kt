package com.kelsos.mbrc.features.radio.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.ui.compose.SingleLineRow
import com.kelsos.mbrc.common.ui.compose.SwipeRefreshScreen
import com.kelsos.mbrc.features.radio.RadioStation
import com.kelsos.mbrc.features.radio.RadioUiMessages
import com.kelsos.mbrc.features.radio.RadioViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun RadioScreen(
  modifier: Modifier = Modifier,
  snackbarHostState: SnackbarHostState,
  viewModel: RadioViewModel = koinViewModel()
) {
  val context = LocalContext.current
  val stations = viewModel.state.radios.collectAsLazyPagingItems()
  var isRefreshing by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.state.events.collect { event ->
      val messageResId = when (event) {
        is RadioUiMessages.QueueFailed -> R.string.radio__play_failed
        is RadioUiMessages.QueueSuccess -> R.string.radio__play_successful
        is RadioUiMessages.RefreshFailed -> {
          isRefreshing = false
          R.string.radio__loading_failed
        }
        is RadioUiMessages.RefreshSuccess -> {
          isRefreshing = false
          R.string.radio__loading_success
        }
        is RadioUiMessages.NetworkUnavailable -> {
          isRefreshing = false
          R.string.connection_error_network_unavailable
        }
      }

      snackbarHostState.showSnackbar(
        message = context.getString(messageResId),
        duration = SnackbarDuration.Short
      )
    }
  }

  SwipeRefreshScreen(
    items = stations,
    isRefreshing = isRefreshing,
    onRefresh = {
      isRefreshing = true
      viewModel.actions.reload()
    },
    modifier = modifier.fillMaxSize(),
    emptyMessage = stringResource(R.string.radio__no_radio_stations),
    emptyIcon = Icons.Default.Radio,
    key = { it.id }
  ) { station ->
    RadioStationItem(
      station = station,
      onPlay = { viewModel.actions.play(it.url) }
    )
  }
}

@Composable
private fun RadioStationItem(
  station: RadioStation,
  onPlay: (RadioStation) -> Unit,
  modifier: Modifier = Modifier
) {
  SingleLineRow(
    text = station.name,
    onClick = { onPlay(station) },
    modifier = modifier,
    leadingContent = {
      Icon(
        imageVector = Icons.Default.Radio,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = MaterialTheme.colorScheme.primary
      )
    },
    trailingContent = {
      IconButton(onClick = { onPlay(station) }) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = stringResource(R.string.radio__play),
          tint = MaterialTheme.colorScheme.primary
        )
      }
    }
  )
}
