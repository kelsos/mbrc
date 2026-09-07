package com.kelsos.mbrc.feature.playback.player.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelsos.mbrc.feature.playback.R
import com.kelsos.mbrc.feature.playback.player.RatingDialogViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBottomSheet(
  isScrobbling: Boolean,
  onScrobbleToggle: () -> Unit,
  onShowTrackDetails: () -> Unit,
  onGoToAlbum: (() -> Unit)?,
  onGoToArtist: (() -> Unit)?,
  onDismiss: () -> Unit,
  isBanned: Boolean,
  isStream: Boolean,
  onBanClick: () -> Unit,
  viewModel: RatingDialogViewModel = koinViewModel()
) {
  val sheetState = rememberModalBottomSheetState()
  val rating by viewModel.rating.collectAsStateWithLifecycle(initialValue = null)
  val halfStarEnabled by viewModel.halfStarEnabled.collectAsStateWithLifecycle()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp)
    ) {
      // Track Details option
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = {
            onDismiss()
            onShowTrackDetails()
          })
          .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Outlined.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(24.dp)
        )
        Text(
          text = stringResource(R.string.track_details_title),
          style = MaterialTheme.typography.bodyLarge
        )
      }

      // Go to Album option
      if (onGoToAlbum != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
              onDismiss()
              onGoToAlbum()
            })
            .padding(vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Album,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = stringResource(R.string.player_go_to_album),
            style = MaterialTheme.typography.bodyLarge
          )
        }
      }

      // Go to Artist option
      if (onGoToArtist != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
              onDismiss()
              onGoToArtist()
            })
            .padding(vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = stringResource(R.string.player_go_to_artist),
            style = MaterialTheme.typography.bodyLarge
          )
        }
      }

      // Ban - grouped with scrobbling because both are Last.fm, and rare enough that it does not
      // need to compete with the track title for room on the player itself.
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(
            enabled = !isStream,
            onClick = {
              onDismiss()
              onBanClick()
            }
          )
          .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.ThumbDown,
          contentDescription = null,
          tint = when {
            isStream -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            isBanned -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          },
          modifier = Modifier.size(24.dp)
        )
        Text(
          text = stringResource(R.string.player_lfm_ban),
          style = MaterialTheme.typography.bodyLarge,
          color = if (isStream) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
          } else {
            MaterialTheme.colorScheme.onSurface
          }
        )
      }

      // Scrobbling toggle
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onScrobbleToggle)
          .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = stringResource(R.string.lastfm_scrobble),
          style = MaterialTheme.typography.bodyLarge
        )
        Switch(
          checked = isScrobbling,
          onCheckedChange = { onScrobbleToggle() }
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Rating section
      Text(
        text = stringResource(R.string.rate_the_playing_track),
        style = MaterialTheme.typography.bodyLarge
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Rating bar with bomb, stars, and clear
      RatingBar(
        rating = rating,
        onRatingChange = { viewModel.changeRating(it) },
        halfStarEnabled = halfStarEnabled,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}
