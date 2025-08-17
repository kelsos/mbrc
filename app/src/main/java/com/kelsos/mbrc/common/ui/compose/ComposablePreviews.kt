package com.kelsos.mbrc.common.ui.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kelsos.mbrc.theme.RemoteTheme

@Preview(name = "Top App Bar - Light")
@Composable
private fun RemoteTopAppBarPreview() {
  RemoteTheme {
    Surface {
      RemoteTopAppBar(
        title = "Music Library",
        navigationIcon = { DrawerNavigationIcon(onClick = {}) },
        actions = { MoreOptionsButton(onClick = {}) }
      )
    }
  }
}

@Preview(name = "Top App Bar - Dark")
@Composable
private fun RemoteTopAppBarDarkPreview() {
  RemoteTheme(darkTheme = true) {
    Surface {
      RemoteTopAppBar(
        title = "Now Playing",
        navigationIcon = { BackNavigationIcon(onClick = {}) },
        actions = { MoreOptionsButton(onClick = {}) }
      )
    }
  }
}

@Preview(name = "Navigation Icons")
@Composable
private fun NavigationIconsPreview() {
  RemoteTheme {
    Surface(modifier = Modifier.padding(16.dp)) {
      androidx.compose.foundation.layout.Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
      ) {
        DrawerNavigationIcon(onClick = {})
        BackNavigationIcon(onClick = {})
        MoreOptionsButton(onClick = {})
      }
    }
  }
}

@Preview(name = "Empty Screen")
@Composable
private fun EmptyScreenPreview() {
  RemoteTheme {
    Surface {
      EmptyScreen(
        message = "No tracks found",
        icon = Icons.Default.MusicNote
      )
    }
  }
}

@Preview(name = "Loading Screen")
@Composable
private fun LoadingScreenPreview() {
  RemoteTheme {
    Surface {
      LoadingScreen(message = "Loading library...")
    }
  }
}

@Preview(name = "Error Screen")
@Composable
private fun ErrorScreenPreview() {
  RemoteTheme {
    Surface {
      ErrorScreen(
        message = "Failed to connect to MusicBee. Please check your connection settings.",
        onRetry = {}
      )
    }
  }
}

@Preview(name = "Single Line Row")
@Composable
private fun SingleLineRowPreview() {
  RemoteTheme {
    Surface {
      androidx.compose.foundation.layout.Column {
        SingleLineRow(
          text = "Bohemian Rhapsody",
          onClick = {},
          leadingContent = {
            androidx.compose.material3.Icon(
              imageVector = Icons.Default.MusicNote,
              contentDescription = null
            )
          },
          trailingContent = {
            androidx.compose.material3.Text("3:54")
          }
        )
        SingleLineRow(
          text = "Another One Bites the Dust",
          onClick = {},
          onLongClick = {},
          leadingContent = {
            androidx.compose.material3.Icon(
              imageVector = Icons.Default.MusicNote,
              contentDescription = null
            )
          }
        )
      }
    }
  }
}

@Preview(name = "Double Line Row")
@Composable
private fun DoubleLineRowPreview() {
  RemoteTheme {
    Surface {
      androidx.compose.foundation.layout.Column {
        DoubleLineRow(
          title = "Bohemian Rhapsody",
          subtitle = "Queen • A Night at the Opera",
          onClick = {},
          leadingContent = {
            androidx.compose.material3.Icon(
              imageVector = Icons.Default.Album,
              contentDescription = null
            )
          },
          trailingContent = {
            androidx.compose.material3.Text("3:54")
          }
        )
        DoubleLineRow(
          title = "Hotel California",
          subtitle = "Eagles • Hotel California",
          onClick = {},
          onLongClick = {}
        )
      }
    }
  }
}

@Preview(name = "Popup Menu")
@Composable
private fun PopupMenuPreview() {
  RemoteTheme {
    Surface {
      var expanded by remember { mutableStateOf(true) }

      androidx.compose.foundation.layout.Box {
        MoreOptionsButton(onClick = { expanded = true })

        PopupMenu(
          expanded = expanded,
          onDismiss = { expanded = false },
          items = listOf(
            PopupMenuItem(
              title = "Edit",
              icon = Icons.Default.Edit,
              onClick = {}
            ),
            PopupMenuItem(
              title = "Delete",
              icon = Icons.Default.Delete,
              onClick = {}
            ),
            PopupMenuItem(
              title = "Share",
              onClick = {}
            )
          )
        )
      }
    }
  }
}

@Preview(name = "Empty State - Light", showBackground = true)
@Composable
private fun EmptyStateLightPreview() {
  RemoteTheme(darkTheme = false) {
    EmptyScreen(
      message = "No albums found in your library",
      icon = Icons.Default.Album
    )
  }
}

@Preview(name = "Empty State - Dark", showBackground = true)
@Composable
private fun EmptyStateDarkPreview() {
  RemoteTheme(darkTheme = true) {
    EmptyScreen(
      message = "No music available",
      icon = Icons.Default.MusicNote
    )
  }
}

@Preview(name = "Loading State", showBackground = true)
@Composable
private fun LoadingStatePreview() {
  RemoteTheme {
    LoadingScreen()
  }
}

@Preview(name = "All Screens Comparison", showBackground = true, heightDp = 600)
@Composable
private fun AllScreensPreview() {
  RemoteTheme {
    androidx.compose.foundation.layout.Row {
      androidx.compose.foundation.layout.Column(
        modifier = Modifier.weight(1f)
      ) {
        LoadingScreen(
          message = "Loading...",
          modifier = Modifier.weight(1f)
        )
      }

      androidx.compose.foundation.layout.Column(
        modifier = Modifier.weight(1f)
      ) {
        EmptyScreen(
          message = "No data",
          modifier = Modifier.weight(1f),
          icon = Icons.Default.MusicNote
        )
      }

      androidx.compose.foundation.layout.Column(
        modifier = Modifier.weight(1f)
      ) {
        ErrorScreen(
          message = "Connection failed",
          onRetry = {},
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}
