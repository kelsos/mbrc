package com.kelsos.mbrc.features.nowplaying

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.kelsos.mbrc.R
import com.kelsos.mbrc.features.dragsort.ItemTouchHelperAdapter
import com.kelsos.mbrc.features.dragsort.OnStartDragListener
import com.kelsos.mbrc.features.dragsort.TouchHelperViewHolder
import com.raizlabs.android.dbflow.kotlinextensions.delete
import com.raizlabs.android.dbflow.kotlinextensions.save
import com.raizlabs.android.dbflow.list.FlowCursorList
import com.raizlabs.android.dbflow.list.FlowCursorList.OnCursorRefreshListener
import timber.log.Timber
import javax.inject.Inject

class NowPlayingAdapter
  @Inject
  constructor(
    context: Activity,
  ) : RecyclerView.Adapter<NowPlayingAdapter.TrackHolder>(),
    ItemTouchHelperAdapter,
    OnCursorRefreshListener<NowPlaying> {
    private val dragStartListener: OnStartDragListener = context as OnStartDragListener

    private var data: FlowCursorList<NowPlaying>? = null
    private var playingTrackIndex: Int = 0
    private var currentTrack: String = ""
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    private var listener: NowPlayingListener? = null

    private fun setPlayingTrack(index: Int) {
      notifyItemChanged(playingTrackIndex)
      this.playingTrackIndex = index
      notifyItemChanged(index)
    }

    fun getPlayingTrackIndex(): Int = this.playingTrackIndex

    fun setPlayingTrack(path: String) {
      val data = this.data ?: return

      this.currentTrack = path
      data.forEachIndexed { index, (_, _, itemPath) ->
        if (itemPath.equals(path)) {
          setPlayingTrack(index)
        }
      }
    }

    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int,
    ): TrackHolder {
      val inflatedView = inflater.inflate(R.layout.ui_list_track_item, parent, false)
      val holder = TrackHolder(inflatedView)
      holder.itemView.setOnClickListener { onClick(holder) }
      holder.container.setOnClickListener { onClick(holder) }
      holder.dragHandle.setOnTouchListener { view, motionEvent ->
        view.performClick()
        if (motionEvent.action == ACTION_DOWN) {
          dragStartListener.onStartDrag(holder)
        }
        return@setOnTouchListener false
      }
      return holder
    }

    private fun onClick(holder: TrackHolder) {
      val listener = this.listener ?: return
      val position = holder.bindingAdapterPosition
      setPlayingTrack(position)
      listener.onPress(position)
    }

    override fun onBindViewHolder(
      holder: TrackHolder,
      position: Int,
    ) {
      val (title, artist) = data?.getItem(position.toLong()) ?: return

      holder.title.text = title
      holder.artist.text = artist
      if (position == playingTrackIndex) {
        holder.trackPlaying.setImageResource(R.drawable.ic_media_now_playing)
      } else {
        holder.trackPlaying.setImageResource(android.R.color.transparent)
      }
    }

    override fun getItemCount(): Int = data?.count?.toInt() ?: 0

    override fun onItemMove(
      from: Int,
      to: Int,
    ): Boolean {
      swapPositions(from, to)
      listener?.onMove(from, to)
      notifyItemMoved(from, to)

      if (currentTrack.isNotBlank()) {
        setPlayingTrack(currentTrack)
      }

      return true
    }

    private fun swapPositions(
      from: Int,
      to: Int,
    ) {
      val data = this.data ?: return

      Timber.v("Swapping %d => %d", from, to)
      val fromTrack = data.getItem(from.toLong()) ?: return
      val toTrack = data.getItem(to.toLong()) ?: return
      Timber.v("from => %s to => %s", fromTrack, toTrack)
      val position = toTrack.position
      toTrack.position = fromTrack.position
      fromTrack.position = position
      toTrack.save()
      fromTrack.save()

      // Before saving remove the listener to avoid interrupting the swapping functionality
      data.removeOnCursorRefreshListener(this)
      data.refresh()
      data.addOnCursorRefreshListener(this)

      Timber.v("after swap => from => %s to => %s", fromTrack, toTrack)
    }

    override fun onItemDismiss(position: Int) {
      val nowPlaying = data?.getItem(position.toLong())

      nowPlaying?.let {
        it.delete()
        refresh()
        notifyItemRemoved(position)
        listener?.onDismiss(position)
      }
    }

    fun refresh() {
      data?.refresh()
    }

    fun update(cursor: FlowCursorList<NowPlaying>) {
      this.data = cursor
      notifyDataSetChanged()
    }

    fun setListener(listener: NowPlayingListener) {
      this.listener = listener
    }

    /**
     * Callback when data refreshes.

     * @param cursorList The object that changed.
     */
    override fun onCursorRefreshed(cursorList: FlowCursorList<NowPlaying>) {
      notifyDataSetChanged()
    }

    interface NowPlayingListener {
      fun onPress(position: Int)

      fun onMove(
        from: Int,
        to: Int,
      )

      fun onDismiss(position: Int)
    }

    class TrackHolder(
      itemView: View,
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
      }
    }
  }
