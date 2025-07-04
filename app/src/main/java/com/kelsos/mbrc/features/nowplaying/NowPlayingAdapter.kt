package com.kelsos.mbrc.features.nowplaying

import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kelsos.mbrc.R
import com.kelsos.mbrc.features.dragsort.ItemTouchHelperAdapter
import com.kelsos.mbrc.features.dragsort.OnStartDragListener
import com.kelsos.mbrc.features.dragsort.TouchHelperViewHolder

interface NowPlayingListener {
  fun onPress(position: Int)

  fun onMove(
    from: Int,
    to: Int,
  )

  fun onDismiss(position: Int)
}

class NowPlayingAdapter :
  PagingDataAdapter<NowPlaying, NowPlayingAdapter.TrackHolder>(DIFF_CALLBACK),
  ItemTouchHelperAdapter {
  private var playingTrackIndex: Int = -1
  private var currentTrack: String = ""
  private var nowPlayingListener: NowPlayingListener? = null
  private var visibleRangeGetter: VisibleRangeGetter? = null
  private var dragStartListener: OnStartDragListener? = null

  private val listener: NowPlayingListener
    get() = checkNotNull(nowPlayingListener) { "NowPlayingListener is not set" }

  private val rangeGetter: VisibleRangeGetter
    get() = checkNotNull(visibleRangeGetter) { "VisibleRangeGetter is not set" }

  private val dragListener: OnStartDragListener
    get() = checkNotNull(dragStartListener) { "DragStartListener is not set" }

  private fun setPlayingTrack(index: Int) {
    notifyItemChanged(playingTrackIndex)
    this.playingTrackIndex = index
    notifyItemChanged(index)
  }

  fun setNowPlayingListener(nowPlayingListener: NowPlayingListener) {
    this.nowPlayingListener = nowPlayingListener
  }

  fun setVisibleRangeGetter(visibleRangeGetter: VisibleRangeGetter) {
    this.visibleRangeGetter = visibleRangeGetter
  }

  fun setDragStartListener(dragStartListener: OnStartDragListener) {
    this.dragStartListener = dragStartListener
  }

  fun getPlayingTrackIndex(): Int = this.playingTrackIndex

  fun setPlayingTrack(path: String) {
    this.currentTrack = path
    val range = rangeGetter.visibleRange()
    notifyItemRangeChanged(
      range.firstItem,
      range.itemCount,
      PLAYING_CHANGED,
    )
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): TrackHolder {
    val inflater = LayoutInflater.from(parent.context)
    val inflatedView = inflater.inflate(R.layout.item_track, parent, false)
    val holder =
      TrackHolder(inflatedView) {
        dragListener.onDragComplete()
      }
    holder.itemView.setOnClickListener { onClick(holder) }
    holder.container.setOnClickListener { onClick(holder) }
    holder.dragHandle.setOnTouchListener { view, motionEvent ->
      view.performClick()
      if (motionEvent.action == ACTION_DOWN) {
        dragListener.onStartDrag(holder)
      }
      return@setOnTouchListener false
    }
    return holder
  }

  private fun onClick(holder: TrackHolder) {
    val listener = this.nowPlayingListener ?: return
    val position = holder.bindingAdapterPosition
    setPlayingTrack(position)
    listener.onPress(position + 1)
  }

  override fun onBindViewHolder(
    holder: TrackHolder,
    position: Int,
    payloads: MutableList<Any>,
  ) {
    val track = getItem(position)
    val isCurrentlyPlaying = track?.path == currentTrack
    if (isCurrentlyPlaying) {
      playingTrackIndex = holder.bindingAdapterPosition
    }

    if (payloads.contains(PLAYING_CHANGED)) {
      holder.setPlaying(isCurrentlyPlaying)
      return
    }
    holder.title.text = track?.title.orEmpty()
    holder.artist.text = track?.artist.orEmpty()
    holder.setPlaying(isCurrentlyPlaying)
  }

  override fun onBindViewHolder(
    holder: TrackHolder,
    position: Int,
  ) {
    onBindViewHolder(holder, position, mutableListOf())
  }

  override fun onItemMove(
    from: Int,
    to: Int,
  ): Boolean {
    notifyItemMoved(from, to)
    listener.onMove(from, to)

    if (currentTrack.isNotBlank()) {
      setPlayingTrack(currentTrack)
    }

    return true
  }

  override fun onItemDismiss(position: Int) {
    listener.onDismiss(position)
  }

  class TrackHolder(
    itemView: View,
    private val onDragEnd: () -> Unit,
  ) : RecyclerView.ViewHolder(itemView),
    TouchHelperViewHolder {
    val title: TextView = itemView.findViewById(R.id.track_title)
    val artist: TextView = itemView.findViewById(R.id.track_artist)
    val trackPlaying: ImageView = itemView.findViewById(R.id.track_indicator_view)
    val container: ConstraintLayout = itemView.findViewById(R.id.track_container)
    val dragHandle: View = itemView.findViewById(R.id.drag_handle)

    override fun onItemSelected() {
      this.itemView.setBackgroundColor(Color.DKGRAY)
    }

    override fun onItemClear() {
      this.itemView.setBackgroundColor(0)
      onDragEnd()
    }

    fun setPlaying(playing: Boolean) {
      if (playing) {
        trackPlaying.setImageResource(R.drawable.baseline_play_arrow_24)
      } else {
        trackPlaying.setImageResource(android.R.color.transparent)
      }
    }
  }

  companion object {
    const val PLAYING_CHANGED = "playing_changed"
    private val DIFF_CALLBACK =
      object : DiffUtil.ItemCallback<NowPlaying>() {
        override fun areItemsTheSame(
          oldItem: NowPlaying,
          newItem: NowPlaying,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
          oldItem: NowPlaying,
          newItem: NowPlaying,
        ): Boolean = oldItem == newItem
      }
  }
}
