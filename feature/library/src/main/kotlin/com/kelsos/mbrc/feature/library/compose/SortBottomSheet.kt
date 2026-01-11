package com.kelsos.mbrc.feature.library.compose

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.ui.theme.RemoteTheme
import com.kelsos.mbrc.feature.library.R

data class SortOption<T>(val field: T, val labelResId: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SortBottomSheet(
  title: String,
  options: List<SortOption<T>>,
  selectedField: T,
  selectedOrder: SortOrder,
  onSortSelected: (field: T, order: SortOrder) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    modifier = modifier
  ) {
    SortBottomSheetContent(
      title = title,
      options = options,
      selectedField = selectedField,
      selectedOrder = selectedOrder,
      onSortSelected = onSortSelected
    )
  }
}

@Composable
fun <T> SortBottomSheetContent(
  title: String,
  options: List<SortOption<T>>,
  selectedField: T,
  selectedOrder: SortOrder,
  onSortSelected: (field: T, order: SortOrder) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .padding(bottom = 32.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(16.dp))

    options.forEach { option ->
      val isSelected = option.field == selectedField
      SortOptionRow(
        label = stringResource(option.labelResId),
        isSelected = isSelected,
        sortOrder = if (isSelected) selectedOrder else null,
        onClick = {
          val newOrder = if (isSelected) {
            if (selectedOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
          } else {
            SortOrder.ASC
          }
          onSortSelected(option.field, newOrder)
        }
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      SortOrderButton(
        label = stringResource(R.string.sort_ascending),
        isSelected = selectedOrder == SortOrder.ASC,
        icon = Icons.Default.ArrowUpward,
        onClick = { onSortSelected(selectedField, SortOrder.ASC) }
      )
      SortOrderButton(
        label = stringResource(R.string.sort_descending),
        isSelected = selectedOrder == SortOrder.DESC,
        icon = Icons.Default.ArrowDownward,
        onClick = { onSortSelected(selectedField, SortOrder.DESC) }
      )
    }
  }
}

@Composable
private fun SortOptionRow(
  label: String,
  isSelected: Boolean,
  sortOrder: SortOrder?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (isSelected) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
      } else {
        Spacer(modifier = Modifier.size(24.dp))
      }
      Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (isSelected) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.onSurface
        }
      )
    }
    if (sortOrder != null) {
      Icon(
        imageVector = if (sortOrder == SortOrder.ASC) {
          Icons.Default.ArrowUpward
        } else {
          Icons.Default.ArrowDownward
        },
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

@Composable
private fun SortOrderButton(
  label: String,
  isSelected: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = if (isSelected) {
        MaterialTheme.colorScheme.primary
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant
      },
      modifier = Modifier.size(20.dp)
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = if (isSelected) {
        MaterialTheme.colorScheme.primary
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant
      }
    )
  }
}

// Preview helpers
private enum class PreviewSortField { NAME, ARTIST, YEAR }

private val previewOptions = listOf(
  SortOption(PreviewSortField.NAME, R.string.sort_by_name),
  SortOption(PreviewSortField.ARTIST, R.string.sort_by_artist),
  SortOption(PreviewSortField.YEAR, R.string.sort_by_year)
)

@Preview(showBackground = true)
@Composable
private fun SortBottomSheetContentPreviewLight() {
  RemoteTheme(darkTheme = false) {
    Surface {
      SortBottomSheetContent(
        title = "Sort by",
        options = previewOptions,
        selectedField = PreviewSortField.NAME,
        selectedOrder = SortOrder.ASC,
        onSortSelected = { _, _ -> }
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun SortBottomSheetContentPreviewDark() {
  RemoteTheme(darkTheme = true) {
    Surface {
      SortBottomSheetContent(
        title = "Sort by",
        options = previewOptions,
        selectedField = PreviewSortField.YEAR,
        selectedOrder = SortOrder.DESC,
        onSortSelected = { _, _ -> }
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun SortBottomSheetContentSingleOptionPreview() {
  RemoteTheme(darkTheme = false) {
    Surface {
      SortBottomSheetContent(
        title = "Sort by",
        options = listOf(SortOption(PreviewSortField.NAME, R.string.sort_by_name)),
        selectedField = PreviewSortField.NAME,
        selectedOrder = SortOrder.DESC,
        onSortSelected = { _, _ -> }
      )
    }
  }
}
