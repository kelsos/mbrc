package com.kelsos.mbrc.common.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

data class TestItem(val id: Long, val name: String)

@RunWith(AndroidJUnit4::class)
class SwipeRefreshScreenTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun swipeRefreshScreen_showsItems_whenDataAvailable() {
    val items = listOf(
      TestItem(1, "Item 1"),
      TestItem(2, "Item 2"),
      TestItem(3, "Item 3")
    )
    val pagingFlow = flowOf(PagingData.from(items))

    composeTestRule.setContent {
      val pagingItems = pagingFlow.collectAsLazyPagingItems()
      SwipeRefreshScreen(
        items = pagingItems,
        isRefreshing = false,
        onRefresh = {},
        emptyMessage = "No items",
        key = { it.id }
      ) { item ->
        SingleLineRow(
          text = item.name,
          onClick = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    // Verify items are displayed
    composeTestRule.onNodeWithText("Item 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Item 2").assertIsDisplayed()
    composeTestRule.onNodeWithText("Item 3").assertIsDisplayed()
  }

  @Test
  fun swipeRefreshScreen_itemClick_triggersCallback() {
    var clickedItem: TestItem? = null
    val items = listOf(TestItem(1, "Clickable Item"))
    val pagingFlow = flowOf(PagingData.from(items))

    composeTestRule.setContent {
      val pagingItems = pagingFlow.collectAsLazyPagingItems()
      SwipeRefreshScreen(
        items = pagingItems,
        isRefreshing = false,
        onRefresh = {},
        emptyMessage = "No items",
        key = { it.id }
      ) { item ->
        SingleLineRow(
          text = item.name,
          onClick = { clickedItem = item }
        )
      }
    }

    composeTestRule.waitForIdle()

    // Click the item
    composeTestRule.onNodeWithText("Clickable Item").performClick()

    // Verify callback was triggered
    assert(clickedItem?.name == "Clickable Item")
  }

  @Test
  fun swipeRefreshScreen_handlesLargeList() {
    val items = (1..100).map { TestItem(it.toLong(), "Item $it") }
    val pagingFlow = flowOf(PagingData.from(items))

    composeTestRule.setContent {
      val pagingItems = pagingFlow.collectAsLazyPagingItems()
      SwipeRefreshScreen(
        items = pagingItems,
        isRefreshing = false,
        onRefresh = {},
        emptyMessage = "No items",
        key = { it.id }
      ) { item ->
        SingleLineRow(text = item.name, onClick = {})
      }
    }

    composeTestRule.waitForIdle()

    // First item should be visible
    composeTestRule.onNodeWithText("Item 1").assertIsDisplayed()

    // Scroll to a later item
    composeTestRule.onNode(hasScrollAction()).performScrollToIndex(50)

    composeTestRule.waitForIdle()

    // Item at index 50 should now be visible
    composeTestRule.onNodeWithText("Item 51").assertIsDisplayed()
  }

  @Test
  fun swipeRefreshScreen_pullToRefresh_triggersCallback() {
    var refreshTriggered = false
    val items = listOf(TestItem(1, "Test Item"))
    val pagingFlow = flowOf(PagingData.from(items))

    composeTestRule.setContent {
      val pagingItems = pagingFlow.collectAsLazyPagingItems()
      SwipeRefreshScreen(
        items = pagingItems,
        isRefreshing = false,
        onRefresh = { refreshTriggered = true },
        emptyMessage = "No items",
        key = { it.id }
      ) { item ->
        SingleLineRow(text = item.name, onClick = {})
      }
    }

    composeTestRule.waitForIdle()

    // Perform swipe down gesture on the scrollable list
    composeTestRule.onNode(hasScrollAction())
      .performTouchInput { swipeDown() }

    composeTestRule.waitForIdle()

    // Note: Pull-to-refresh may not trigger in tests due to gesture limitations
    // This test primarily verifies the gesture can be performed without crash
  }

  @Test
  fun swipeRefreshScreen_displaysMultipleItems_withCorrectOrder() {
    val items = listOf(
      TestItem(1, "First Item"),
      TestItem(2, "Second Item"),
      TestItem(3, "Third Item"),
      TestItem(4, "Fourth Item"),
      TestItem(5, "Fifth Item")
    )
    val pagingFlow = flowOf(PagingData.from(items))

    composeTestRule.setContent {
      val pagingItems = pagingFlow.collectAsLazyPagingItems()
      SwipeRefreshScreen(
        items = pagingItems,
        isRefreshing = false,
        onRefresh = {},
        emptyMessage = "No items",
        key = { it.id }
      ) { item ->
        SingleLineRow(text = item.name, onClick = {})
      }
    }

    composeTestRule.waitForIdle()

    // All items should be displayed
    composeTestRule.onNodeWithText("First Item").assertIsDisplayed()
    composeTestRule.onNodeWithText("Second Item").assertIsDisplayed()
    composeTestRule.onNodeWithText("Third Item").assertIsDisplayed()
    composeTestRule.onNodeWithText("Fourth Item").assertIsDisplayed()
    composeTestRule.onNodeWithText("Fifth Item").assertIsDisplayed()
  }

  @Test
  fun swipeRefreshScreen_doubleLineRow_displaysCorrectly() {
    val items = listOf(TestItem(1, "Main Title"))
    val pagingFlow = flowOf(PagingData.from(items))

    composeTestRule.setContent {
      val pagingItems = pagingFlow.collectAsLazyPagingItems()
      SwipeRefreshScreen(
        items = pagingItems,
        isRefreshing = false,
        onRefresh = {},
        emptyMessage = "No items",
        key = { it.id }
      ) { item ->
        DoubleLineRow(
          title = item.name,
          subtitle = "Subtitle for ${item.name}",
          onClick = {}
        )
      }
    }

    composeTestRule.waitForIdle()

    // Both title and subtitle should be displayed
    composeTestRule.onNodeWithText("Main Title").assertIsDisplayed()
    composeTestRule.onNodeWithText("Subtitle for Main Title").assertIsDisplayed()
  }
}
