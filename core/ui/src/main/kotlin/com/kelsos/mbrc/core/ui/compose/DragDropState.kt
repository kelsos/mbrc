package com.kelsos.mbrc.core.ui.compose

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * State holder for drag and drop functionality in a LazyColumn.
 *
 * Based on the official AndroidX LazyColumnDragAndDropDemo.kt from the Compose Foundation library.
 * Source: https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:
 * compose/foundation/foundation/integration-tests/foundation-demos/src/main/java/
 * androidx/compose/foundation/demos/LazyColumnDragAndDropDemo.kt
 *
 * @param state The LazyListState of the LazyColumn
 * @param scope CoroutineScope used to clear the post-drop permutation once
 * the new paging data has had a chance to land
 * @param onMove Callback invoked when items are swapped during drag
 * @param onDragEnd Callback invoked when drag operation completes (for syncing to server)
 */
@Stable
class DragDropState(
  private val state: LazyListState,
  private val scope: CoroutineScope,
  private val onMove: (Int, Int) -> Unit,
  private val onDragEnd: () -> Unit
) {
  var draggingItemIndex by mutableStateOf<Int?>(null)
    private set

  /**
   * True between drag-release and the post-drop paging emission. While set, the
   * permutation indices stay populated so the dropped item remains rendered at
   * its target slot, avoiding a visible snap-back to the source position before
   * `dao.move` propagates through paging.
   */
  var isSettling by mutableStateOf(false)
    private set

  /**
   * Position of the dragged item at the moment the drag started. Stays fixed
   * for the entire drag (unlike [draggingItemIndex], which tracks the current
   * displayed position). Used by the renderer to compute the displayed
   * permutation without mutating the underlying data source.
   */
  var dragSourceIndex by mutableStateOf<Int?>(null)
    private set

  internal val scrollChannel = Channel<Float>()

  private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
  private var draggingItemInitialOffset by mutableIntStateOf(0)
  private var settleVersion by mutableIntStateOf(0)

  val draggingItemOffset: Float
    get() = if (isSettling) {
      0f
    } else {
      draggingItemLayoutInfo?.let { item ->
        draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
      } ?: 0f
    }

  private val draggingItemLayoutInfo: LazyListItemInfo?
    get() = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }

  internal fun onDragStart(offset: Offset) {
    state.layoutInfo.visibleItemsInfo
      .firstOrNull { item -> offset.y.toInt() in item.offset..item.offset + item.size }
      ?.let(::beginDrag)
  }

  internal fun onDragStartAtIndex(index: Int) {
    state.layoutInfo.visibleItemsInfo
      .firstOrNull { it.index == index }
      ?.let(::beginDrag)
  }

  // Invalidates any pending settle from a previous drop so it doesn't clobber
  // the new drag's state.
  private fun beginDrag(item: LazyListItemInfo) {
    settleVersion++
    isSettling = false
    draggingItemIndex = item.index
    dragSourceIndex = item.index
    draggingItemInitialOffset = item.offset
  }

  internal fun onDragInterrupted() {
    val hadDrag = draggingItemIndex != null
    draggingItemDraggedDelta = 0f
    draggingItemInitialOffset = 0
    if (hadDrag) {
      // Keep draggingItemIndex / dragSourceIndex populated so the permutation
      // continues to render the dropped item at its target slot until paging
      // emits the new ordering after onDragEnd's dao.move.
      isSettling = true
      val version = ++settleVersion
      onDragEnd()
      scope.launch {
        delay(SETTLE_TIMEOUT_MS)
        if (version == settleVersion) {
          isSettling = false
          draggingItemIndex = null
          dragSourceIndex = null
        }
      }
    } else {
      draggingItemIndex = null
      dragSourceIndex = null
    }
  }

  private companion object {
    const val SETTLE_TIMEOUT_MS = 100L
  }

  internal fun onDrag(offset: Offset) {
    draggingItemDraggedDelta += offset.y

    val draggingItem = draggingItemLayoutInfo ?: return
    val startOffset = draggingItem.offset + draggingItemOffset
    val endOffset = startOffset + draggingItem.size
    val middleOffset = startOffset + (endOffset - startOffset) / 2f

    val targetItem = state.layoutInfo.visibleItemsInfo.find { item ->
      middleOffset.toInt() in item.offset..item.offsetEnd &&
        draggingItem.index != item.index
    }

    if (targetItem != null) {
      if (
        draggingItem.index == state.firstVisibleItemIndex ||
        targetItem.index == state.firstVisibleItemIndex
      ) {
        state.requestScrollToItem(
          state.firstVisibleItemIndex,
          state.firstVisibleItemScrollOffset
        )
      }
      onMove.invoke(draggingItem.index, targetItem.index)
      draggingItemIndex = targetItem.index
    } else {
      val overscroll = when {
        draggingItemDraggedDelta > 0 ->
          (endOffset - state.layoutInfo.viewportEndOffset).coerceAtLeast(0f)

        draggingItemDraggedDelta < 0 ->
          (startOffset - state.layoutInfo.viewportStartOffset).coerceAtMost(0f)

        else -> 0f
      }
      if (overscroll != 0f) {
        scrollChannel.trySend(overscroll)
      }
    }
  }

  private val LazyListItemInfo.offsetEnd: Int
    get() = this.offset + this.size
}

/**
 * Creates and remembers a [DragDropState] for managing drag and drop in a LazyColumn.
 *
 * @param lazyListState The LazyListState of the LazyColumn
 * @param onMove Callback invoked when items are swapped during drag
 * @param onDragEnd Callback invoked when drag operation completes
 */
@Composable
fun rememberDragDropState(
  lazyListState: LazyListState,
  onMove: (Int, Int) -> Unit,
  onDragEnd: () -> Unit
): DragDropState {
  val scope = rememberCoroutineScope()
  val state = remember(lazyListState) {
    DragDropState(
      state = lazyListState,
      scope = scope,
      onMove = onMove,
      onDragEnd = onDragEnd
    )
  }

  LaunchedEffect(state) {
    while (true) {
      val diff = state.scrollChannel.receive()
      lazyListState.scrollBy(diff)
    }
  }

  return state
}

/**
 * Maps a LazyColumn slot to the source-list index it should display, given
 * the in-flight drag's [source] (where the drag started) and [target] (where
 * the dragged item currently sits).
 *
 * This lets the LazyColumn render the visual reorder during a drag without
 * mutating the underlying data — the dragged item appears at [target], items
 * between [source] and [target] shift one slot toward [source], everything
 * else stays put. On drag-end, the permutation collapses (source/target are
 * cleared) and the next data emission carries the persisted order.
 *
 * Returns [slot] unchanged when no drag is in progress.
 */
fun displayedSourceIndex(slot: Int, source: Int?, target: Int?): Int {
  if (source == null || target == null || source == target) return slot
  return when {
    slot == target -> source
    source < target && slot in source until target -> slot + 1
    source > target && slot in target + 1..source -> slot - 1
    else -> slot
  }
}

/**
 * Modifier that enables drag detection on a container (typically applied to LazyColumn).
 * Detects long press followed by drag gestures.
 */
fun Modifier.dragContainer(dragDropState: DragDropState): Modifier = pointerInput(dragDropState) {
  detectDragGesturesAfterLongPress(
    onDrag = { change, offset ->
      change.consume()
      dragDropState.onDrag(offset = offset)
    },
    onDragStart = { offset -> dragDropState.onDragStart(offset) },
    onDragEnd = { dragDropState.onDragInterrupted() },
    onDragCancel = { dragDropState.onDragInterrupted() }
  )
}

/**
 * Modifier that enables drag detection on a per-item handle. Unlike [dragContainer],
 * which lives on the LazyColumn and infers the dragged index from the touch offset,
 * this attaches to a specific row's handle and starts the drag with the known [index].
 * This keeps drag gestures isolated to the handle so they don't compete with row-level
 * swipe-to-dismiss or scroll gestures.
 */
@Composable
fun Modifier.dragHandle(dragDropState: DragDropState, index: Int): Modifier {
  // Read index via rememberUpdatedState so the pointer-input coroutine isn't
  // torn down and re-created every time a reorder shifts a row's slot.
  val currentIndex = rememberUpdatedState(index)
  return this.pointerInput(dragDropState) {
    detectDragGesturesAfterLongPress(
      onDrag = { change, offset ->
        change.consume()
        dragDropState.onDrag(offset = offset)
      },
      onDragStart = { dragDropState.onDragStartAtIndex(currentIndex.value) },
      onDragEnd = { dragDropState.onDragInterrupted() },
      onDragCancel = { dragDropState.onDragInterrupted() }
    )
  }
}
