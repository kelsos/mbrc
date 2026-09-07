package com.kelsos.mbrc.core.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CoroutineScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The reorder half of [DragDropState], which [DragDropStateSettleTest] leaves alone.
 *
 * The host lays out 40dp rows at density 1, so a row occupies pixels `index * 40` to
 * `index * 40 + 40` and [ITEM_HEIGHT_PX] converts a row count into a drag distance. A swap happens
 * when the dragged row's midpoint crosses into a neighbour, so a drag of one full row height is the
 * smallest move that reorders.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DragDropStateReorderTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val moves = mutableListOf<Pair<Int, Int>>()
  private var dragEnds = 0

  @Test
  fun `dragging a row down a full row swaps it with the one below`() {
    val dragDrop = mountAndStartDrag(index = 1)

    drag(dragDrop, ITEM_HEIGHT_PX)

    assertThat(moves).containsExactly(1 to 2)
    assertThat(dragDrop.draggingItemIndex).isEqualTo(2)
  }

  @Test
  fun `dragging a row up a full row swaps it with the one above`() {
    val dragDrop = mountAndStartDrag(index = 2)

    drag(dragDrop, -ITEM_HEIGHT_PX)

    assertThat(moves).containsExactly(2 to 1)
    assertThat(dragDrop.draggingItemIndex).isEqualTo(1)
  }

  @Test
  fun `a drag shorter than half a row does not reorder anything`() {
    val dragDrop = mountAndStartDrag(index = 1)

    drag(dragDrop, ITEM_HEIGHT_PX / 2 - 1)

    assertWithMessage("the midpoint has not left the row yet, so there is nothing to swap")
      .that(moves)
      .isEmpty()
    assertThat(dragDrop.draggingItemIndex).isEqualTo(1)
  }

  @Test
  fun `the drag source stays put while the dragged index follows the row`() {
    val dragDrop = mountAndStartDrag(index = 1)

    drag(dragDrop, ITEM_HEIGHT_PX)
    drag(dragDrop, ITEM_HEIGHT_PX)

    assertThat(moves).containsExactly(1 to 2, 2 to 3).inOrder()
    assertWithMessage("the permutation is computed against where the drag began")
      .that(dragDrop.dragSourceIndex)
      .isEqualTo(1)
    assertThat(dragDrop.draggingItemIndex).isEqualTo(3)
  }

  @Test
  fun `a drag handle starts the drag at the index it was given`() {
    val dragDrop = mountWithoutDrag()

    composeTestRule.runOnUiThread { dragDrop.onDragStartAtIndex(3) }
    composeTestRule.waitForIdle()

    assertThat(dragDrop.dragSourceIndex).isEqualTo(3)
    assertThat(dragDrop.draggingItemIndex).isEqualTo(3)
  }

  @Test
  fun `a drag started off any row leaves the state idle`() {
    val dragDrop = mountWithoutDrag()

    composeTestRule.runOnUiThread { dragDrop.onDragStart(Offset(0f, -1f)) }
    composeTestRule.waitForIdle()

    assertThat(dragDrop.dragSourceIndex).isNull()
    assertThat(dragDrop.draggingItemIndex).isNull()
  }

  @Test
  fun `dragging without a started drag is ignored`() {
    val dragDrop = mountWithoutDrag()

    drag(dragDrop, ITEM_HEIGHT_PX)

    assertThat(moves).isEmpty()
    assertThat(dragDrop.draggingItemIndex).isNull()
  }

  @Test
  fun `the drop reports the reorder once, not once per swap`() {
    val dragDrop = mountAndStartDrag(index = 1)

    drag(dragDrop, ITEM_HEIGHT_PX)
    drag(dragDrop, ITEM_HEIGHT_PX)
    composeTestRule.runOnUiThread { dragDrop.onDragInterrupted() }
    composeTestRule.waitForIdle()

    assertThat(moves).hasSize(2)
    assertThat(dragEnds).isEqualTo(1)
  }

  private fun drag(dragDrop: DragDropState, deltaY: Float) {
    composeTestRule.runOnUiThread { dragDrop.onDrag(Offset(0f, deltaY)) }
    composeTestRule.waitForIdle()
  }

  private fun mountWithoutDrag(): DragDropState {
    lateinit var listState: LazyListState
    lateinit var scope: CoroutineScope
    composeTestRule.setContent {
      listState = rememberLazyListState()
      scope = rememberCoroutineScope()
      ReorderTestHost(listState = listState)
    }
    composeTestRule.waitForIdle()
    return DragDropState(
      state = listState,
      scope = scope,
      onMove = { from, to -> moves.add(from to to) },
      onDragEnd = { dragEnds++ }
    )
  }

  private fun mountAndStartDrag(index: Int): DragDropState {
    val dragDrop = mountWithoutDrag()
    composeTestRule.runOnUiThread { dragDrop.onDragStartAtIndex(index) }
    composeTestRule.waitForIdle()
    return dragDrop
  }

  private companion object {
    const val ITEM_HEIGHT_PX = 40f
  }
}

@Composable
private fun ReorderTestHost(listState: LazyListState) {
  LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
    items(count = 20, key = { it }) { index ->
      Text(
        text = "Item $index",
        modifier = Modifier
          .fillMaxWidth()
          .height(40.dp)
      )
    }
  }
}
