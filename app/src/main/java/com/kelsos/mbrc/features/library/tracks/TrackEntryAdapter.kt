package com.kelsos.mbrc.features.library.tracks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isGone
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.size.Scale
import com.kelsos.mbrc.R
import com.kelsos.mbrc.features.library.MenuItemSelectedListener
import com.kelsos.mbrc.features.library.popupMenu
import java.io.File

class TrackEntryAdapter : PagingDataAdapter<Track, TrackEntryAdapter.ViewHolder>(DIFF_CALLBACK) {
  private var listener: MenuItemSelectedListener<Track>? = null
  private var coverMode: Boolean = false

  fun setCoverMode(enabled: Boolean) {
    coverMode = enabled
  }

  fun setMenuItemSelectedListener(listener: MenuItemSelectedListener<Track>) {
    this.listener = listener
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): ViewHolder {
    val inflater: LayoutInflater = LayoutInflater.from(parent.context)
    val view = inflater.inflate(R.layout.ui_list_dual, parent, false)
    val holder = ViewHolder(view)
    holder.indicator.setOnClickListener {
      it.popupMenu(R.menu.popup_track) {
        val position = holder.bindingAdapterPosition
        getItem(position)?.let { track ->
          listener?.onAction(track, it)
        }
      }
    }
    holder.itemView.setOnClickListener {
      val position = holder.bindingAdapterPosition
      getItem(position)?.let { track ->
        listener?.onAction(track)
      }
    }
    return holder
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int,
  ) {
    val cache = File(holder.itemView.context.cacheDir, "covers")
    getItem(position)?.let { entry ->
      holder.title.text = entry.title
      val artist = entry.artist
      holder.artist.text = artist.ifBlank { holder.unknownArtist }
      holder.image.isGone = !coverMode
      holder.image.load(File(cache, entry.key())) {
        crossfade(false)
        placeholder(R.drawable.ic_image_no_cover)
        error(R.drawable.ic_image_no_cover)
        size(
          holder.itemView.context.resources
            .getDimensionPixelSize(R.dimen.list_album_size),
        )
        scale(Scale.FILL)
      }
    }
  }

  class ViewHolder(
    itemView: View,
  ) : RecyclerView.ViewHolder(itemView) {
    val artist: TextView = itemView.findViewById(R.id.line_two)
    val title: TextView = itemView.findViewById(R.id.line_one)
    val indicator: LinearLayout = itemView.findViewById(R.id.ui_item_context_indicator)
    val unknownArtist: String = itemView.context.getString(R.string.unknown_artist)
    val image: ImageView = itemView.findViewById(R.id.cover)
  }

  companion object {
    private val DIFF_CALLBACK =
      object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(
          oldItem: Track,
          newItem: Track,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
          oldItem: Track,
          newItem: Track,
        ): Boolean = oldItem == newItem
      }
  }
}
