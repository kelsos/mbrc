package com.kelsos.mbrc.screenshots

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.kelsos.mbrc.common.ui.compose.EmptyScreen
import com.kelsos.mbrc.common.ui.compose.SingleLineRow
import com.kelsos.mbrc.theme.RemoteTheme

@PreviewTest
@Preview(showBackground = true)
@Composable
fun RadioStationItemLight() {
  RemoteTheme(darkTheme = false) {
    Surface {
      SingleLineRow(
        text = "1.FM - Absolute 90s Party Zone",
        onClick = {},
        leadingContent = {
          Icon(
            imageVector = Icons.Default.Radio,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
          )
        },
        trailingContent = {
          IconButton(onClick = {}) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = "Play",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      )
    }
  }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun RadioStationItemDark() {
  RemoteTheme(darkTheme = true) {
    Surface {
      SingleLineRow(
        text = "1.FM - Absolute 90s Party Zone",
        onClick = {},
        leadingContent = {
          Icon(
            imageVector = Icons.Default.Radio,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
          )
        },
        trailingContent = {
          IconButton(onClick = {}) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = "Play",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      )
    }
  }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun EmptyRadioScreenLight() {
  RemoteTheme(darkTheme = false) {
    Surface(modifier = Modifier.fillMaxSize()) {
      EmptyScreen(
        message = "No radio stations found",
        icon = Icons.Default.Radio
      )
    }
  }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun EmptyRadioScreenDark() {
  RemoteTheme(darkTheme = true) {
    Surface(modifier = Modifier.fillMaxSize()) {
      EmptyScreen(
        message = "No radio stations found",
        icon = Icons.Default.Radio
      )
    }
  }
}
