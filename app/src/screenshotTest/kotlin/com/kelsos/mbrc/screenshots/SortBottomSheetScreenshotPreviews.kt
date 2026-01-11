package com.kelsos.mbrc.screenshots

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.ui.theme.RemoteTheme
import com.kelsos.mbrc.feature.library.R
import com.kelsos.mbrc.feature.library.compose.SortBottomSheetContent
import com.kelsos.mbrc.feature.library.compose.SortOption

private enum class PreviewSortField { NAME, ARTIST, YEAR }

private val albumSortOptions = listOf(
  SortOption(PreviewSortField.NAME, R.string.sort_by_name),
  SortOption(PreviewSortField.ARTIST, R.string.sort_by_artist),
  SortOption(PreviewSortField.YEAR, R.string.sort_by_year)
)

private val singleSortOption = listOf(
  SortOption(PreviewSortField.NAME, R.string.sort_by_name)
)

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SortBottomSheetNameAscLight() {
  RemoteTheme(darkTheme = false) {
    Surface {
      SortBottomSheetContent(
        title = "Sort by",
        options = albumSortOptions,
        selectedField = PreviewSortField.NAME,
        selectedOrder = SortOrder.ASC,
        onSortSelected = { _, _ -> }
      )
    }
  }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SortBottomSheetNameAscDark() {
  RemoteTheme(darkTheme = true) {
    Surface {
      SortBottomSheetContent(
        title = "Sort by",
        options = albumSortOptions,
        selectedField = PreviewSortField.NAME,
        selectedOrder = SortOrder.ASC,
        onSortSelected = { _, _ -> }
      )
    }
  }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SortBottomSheetYearDescLight() {
  RemoteTheme(darkTheme = false) {
    Surface {
      SortBottomSheetContent(
        title = "Sort by",
        options = albumSortOptions,
        selectedField = PreviewSortField.YEAR,
        selectedOrder = SortOrder.DESC,
        onSortSelected = { _, _ -> }
      )
    }
  }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SortBottomSheetYearDescDark() {
  RemoteTheme(darkTheme = true) {
    Surface {
      SortBottomSheetContent(
        title = "Sort by",
        options = albumSortOptions,
        selectedField = PreviewSortField.YEAR,
        selectedOrder = SortOrder.DESC,
        onSortSelected = { _, _ -> }
      )
    }
  }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SortBottomSheetSingleOptionLight() {
  RemoteTheme(darkTheme = false) {
    Surface {
      SortBottomSheetContent(
        title = "Sort by",
        options = singleSortOption,
        selectedField = PreviewSortField.NAME,
        selectedOrder = SortOrder.ASC,
        onSortSelected = { _, _ -> }
      )
    }
  }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SortBottomSheetSingleOptionDark() {
  RemoteTheme(darkTheme = true) {
    Surface {
      SortBottomSheetContent(
        title = "Sort by",
        options = singleSortOption,
        selectedField = PreviewSortField.NAME,
        selectedOrder = SortOrder.DESC,
        onSortSelected = { _, _ -> }
      )
    }
  }
}
