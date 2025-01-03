package com.kelsos.mbrc.features.playlists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kelsos.mbrc.R

class PlaylistAdapter : PagingDataAdapter<Playlist, PlaylistAdapter.ViewHolder>(DIFF_CALLBACK) {
  private var playlistPressedListener: OnPlaylistPressedListener? = null

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val view = inflater.inflate(R.layout.listitem_single, parent, false)
    val viewHolder = ViewHolder(view)

    viewHolder.itemView.setOnClickListener {
      val position = viewHolder.bindingAdapterPosition
      val path = getItem(position)?.url
      if (path != null) {
        playlistPressedListener?.playlistPressed(path)
      }
    }
    return viewHolder
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int,
  ) {
    val playlist = getItem(position)
    if (playlist != null) {
      holder.name.text = playlist.name
    }
    holder.context.visibility = View.GONE
  }

  fun setPlaylistPressedListener(playlistPressedListener: OnPlaylistPressedListener?) {
    this.playlistPressedListener = playlistPressedListener
  }

  fun interface OnPlaylistPressedListener {
    fun playlistPressed(path: String)
  }

  class ViewHolder(
    itemView: View,
  ) : RecyclerView.ViewHolder(itemView) {
    val name: TextView = itemView.findViewById(R.id.line_one)
    val context: LinearLayout = itemView.findViewById(R.id.ui_item_context_indicator)
  }

  companion object {
    private val DIFF_CALLBACK =
      object : DiffUtil.ItemCallback<Playlist>() {
        override fun areContentsTheSame(
          oldItem: Playlist,
          newItem: Playlist,
        ): Boolean = oldItem.id == newItem.id

        override fun areItemsTheSame(
          oldItem: Playlist,
          newItem: Playlist,
        ): Boolean = oldItem == newItem
      }
  }
}
